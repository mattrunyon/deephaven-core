---
title: Iceberg and Deephaven
sidebar_label: Iceberg
---

[Apache Iceberg](https://iceberg.apache.org/) is a high-performance format for tabular data. Deephaven's Iceberg integration enables users to interact with Iceberg catalogs, namespaces, tables, and snapshots. This guide walks through reading from Iceberg with a single table and snapshot, then writes multiple Deephaven tables to the same Iceberg namespace. The examples presented this guide interact with a [REST catalog](https://www.tabular.io/apache-iceberg-cookbook/getting-started-catalog-background/).

The API enables you to interact with many types of catalogs. They include:

- REST
- AWS Glue
- JDBC
- Hive
- Hadoop
- Nessie

> [!NOTE]
> Some catalog types in the list above require adding dependencies to your classpath.

## Deephaven's Iceberg package

Deephaven's Iceberg integration is provided in the form of six different packages, all prefixed by `io.deephaven.iceberg`. These packages are:

- [`io.deephaven.iceberg.base`](/core/javadoc/io/deephaven/iceberg/base/package-summary.html)
- [`io.deephaven.iceberg.internal`](/core/javadoc/io/deephaven/iceberg/internal/package-summary.html)
- [`io.deephaven.iceberg.layout`](/core/javadoc/io/deephaven/iceberg/layout/package-summary.html)
- [`io.deephaven.iceberg.location`](/core/javadoc/io/deephaven/iceberg/location/package-summary.html)
- [`io.deephaven.iceberg.relative`](/core/javadoc/io/deephaven/iceberg/relative/package-summary.html)
- [`io.deephaven.iceberg.util`](/core/javadoc/io/deephaven/iceberg/util/package-summary.html)

The examples presented in this guide only use [`io.deephaven.iceberg.util`](/core/javadoc/io/deephaven/iceberg/util/package-summary.html). The others are provided for visibility.

When querying Iceberg tables located in any S3-compatible storage provider, the [`io.deephaven.extensions.s3`](/core/javadoc/io/deephaven/extensions/s3/package-summary.html) package is also required.

## A Deephaven deployment for Iceberg

The examples presented in this guide use an Iceberg REST catalog. This section closely follows Iceberg's [Spark quickstart](https://iceberg.apache.org/spark-quickstart/). It extends the `docker-compose.yml` file in that guide to include Deephaven as part of the Iceberg Docker network. The Deephaven server starts alongside a Spark server, Iceberg REST API, and MinIO object store.

<details>
<summary>docker-compose.yml</summary>

```yaml
services:
  spark-iceberg:
    image: tabulario/spark-iceberg
    container_name: spark-iceberg
    build: spark/
    networks:
      iceberg_net:
    depends_on:
      - rest
      - minio
    volumes:
      - ./warehouse:/home/iceberg/warehouse
      - ./notebooks:/home/iceberg/notebooks/notebooks
    environment:
      - AWS_ACCESS_KEY_ID=admin
      - AWS_SECRET_ACCESS_KEY=password
      - AWS_REGION=us-east-1
    ports:
      - 8888:8888
      - 8081:8080
      - 11000:10000
      - 11001:10001
  rest:
    image: tabulario/iceberg-rest
    container_name: iceberg-rest
    networks:
      iceberg_net:
    ports:
      - 8181:8181
    environment:
      - AWS_ACCESS_KEY_ID=admin
      - AWS_SECRET_ACCESS_KEY=password
      - AWS_REGION=us-east-1
      - CATALOG_WAREHOUSE=s3://warehouse/
      - CATALOG_IO__IMPL=org.apache.iceberg.aws.s3.S3FileIO
      - CATALOG_S3_ENDPOINT=http://minio:9000
  minio:
    image: minio/minio
    container_name: minio
    environment:
      - MINIO_ROOT_USER=admin
      - MINIO_ROOT_PASSWORD=password
      - MINIO_DOMAIN=minio
    networks:
      iceberg_net:
        aliases:
          - warehouse.minio
    ports:
      - 9001:9001
      - 9000:9000
    command: ["server", "/data", "--console-address", ":9001"]
  mc:
    depends_on:
      - minio
    image: minio/mc
    container_name: mc
    networks:
      iceberg_net:
    environment:
      - AWS_ACCESS_KEY_ID=admin
      - AWS_SECRET_ACCESS_KEY=password
      - AWS_REGION=us-east-1
    entrypoint: >
      /bin/sh -c "
      until (/usr/bin/mc config host add minio http://minio:9000 admin password) do echo '...waiting...' && sleep 1; done;
      /usr/bin/mc mb minio/warehouse;
      /usr/bin/mc policy set public minio/warehouse;
      tail -f /dev/null
      "
  deephaven:
    image: ghcr.io/deephaven/server-slim:latest
    networks:
      iceberg_net:
    ports:
      - "${DEEPHAVEN_PORT:-10000}:10000"
    environment:
      - START_OPTS=-Dauthentication.psk=YOUR_PASSWORD_HERE
      - USER
    volumes:
      - ./data:/data
      - /home/${USER}/.aws:/home/${USER}/.aws
networks:
  iceberg_net:
```

</details>

> [!IMPORTANT]
> The `docker-compose.yml` file above sets the pre-shared key to `YOUR_PASSWORD_HERE`. This doesn't meet security best practices and should be changed in a production environment. For more information, see [pre-shared key authentication](../authentication/auth-psk.md).

Run `docker compose up` from the directory with the `docker-compose.yml` file. This starts the Deephaven server, Spark server, Iceberg REST API, and MinIO object store. When you're finished, a `ctrl+C` or `docker compose down` stops the containers.

### Create an Iceberg catalog

This section follows the Iceberg [Spark quickstart](https://iceberg.apache.org/spark-quickstart/) by creating an Iceberg catalog with a single table and snapshot using the Iceberg REST API in Jupyter. The [docker-compose.yml](#a-deephaven-deployment-for-iceberg) extends the one in the Spark quickstart guide to include Deephaven as a service in the Iceberg Docker network. As such, the file starts up the following services:

- MinIO object store
- MinIO client
- Iceberg Spark server, reachable by Jupyter
- Deephaven server

Once the Docker containers are up and running, head to `http://localhost:8888` to access the Iceberg Spark server in Jupyter. Open either the `Iceberg - Getting Started` or `PyIceberg - Getting Started` notebooks, which create a catalog using the Iceberg REST API. The first four code blocks create an Iceberg table called `nyc.taxis`. Run this code to follow along with this guide, which uses the table in the sections below. All code blocks afterward are optional for our purposes.

## Interact with the Iceberg catalog

After creating the Iceberg catalog and table, head to the [Deephaven IDE](http://localhost:10000/ide).

To interact with an Iceberg catalog, you must first create an [`IcebergCatalogAdapter`](/core/javadoc/io/deephaven/iceberg/util/IcebergCatalogAdapter.html) instance. Since this guide uses a REST catalog, the adapter can be created using the generic [`createAdapter`](https://deephaven.io/core/javadoc/io/deephaven/iceberg/util/IcebergTools.html#createAdapter(org.apache.iceberg.catalog.Catalog)) method:

```groovy skip-test
import io.deephaven.iceberg.util.*

restAdapter = IcebergTools.createAdapter(
    "minio-iceberg",
    Map.of(
        "type", "rest",
        "uri", "http://rest:8181",
        "client.region", "us-east-1",
        "s3.access-key-id", "admin",
        "s3.secret-access-key", "password",
        "s3.endpoint", "http://minio:9000",
        "io-impl", "org.apache.iceberg.aws.s3.S3FileIO"
    )
)
```

If you are working with a REST catalog backed by S3 storage, you can use the more specific [`createS3Rest`](https://deephaven.io/core/javadoc/io/deephaven/iceberg/util/IcebergToolsS3.html#createS3Rest(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String)) method:

```groovy docker-config=iceberg test-set=1 order=null
import io.deephaven.iceberg.util.*

restAdapter = IcebergToolsS3.createS3Rest(
    "minio-iceberg",        // catalog name
    "http://rest:8181",     // catalog URI
    "s3a://warehouse/wh",   // warehouse location
    "us-east-1",            // region name
    "admin",                // access key ID
    "password",             // secret access key
    "http://minio:9000",    // endpoint override
)
```

Once an [`IcebergCatalogAdapter`](/core/javadoc/io/deephaven/iceberg/util/IcebergCatalogAdapter.html) has been created, it can query the namespaces and tables in a catalog. The following code block gets the top-level namespaces and tables in the `nyc` namespace:

```groovy docker-config=iceberg test-set=1 order=namespaces,tables
namespaces = restAdapter.namespaces()
tables = restAdapter.tables("nyc")
```

### Load an Iceberg table into Deephaven

To load the `nyc.taxis` Iceberg table into Deephaven, start by creating an instance of [`IcebergReadInstructions`](/core/javadoc/io/deephaven/iceberg/util/IcebergReadInstructions.html) via the builder. Since the table doesn't change, the instructions tell Deephaven that it's static:

```groovy docker-config=iceberg test-set=1 order=null
staticInstructions = IcebergReadInstructions.builder()
    .updateMode(IcebergUpdateMode.staticMode())
    .build()
```

This is an optional argument with the default being `static`. See [`IcebergReadInstructions`](/core/javadoc/io/deephaven/iceberg/util/IcebergReadInstructions.html) for more information.

At this point, you can load a table from the catalog with [`loadTable`](https://deephaven.io/core/javadoc/io/deephaven/iceberg/util/IcebergCatalogAdapter.html#loadTable(java.lang.String)). This returns an [`IcebergTableAdapter`](/core/javadoc/io/deephaven/iceberg/util/IcebergTableAdapter.html) rather than a Deephaven table. The table adapter provides you with several methods to read from or write to the underlying Iceberg table.

```groovy docker-config=iceberg test-set=1 order=null
icebergTaxis = restAdapter.loadTable("nyc.taxis")
```

With the table adapter and instructions in hand, the Iceberg table can be read into a Deephaven table.

```groovy docker-config=iceberg test-set=1 order=taxis
taxis = icebergTaxis.table(staticInstructions)
```

### Write Deephaven tables to Iceberg

To write one or more Deephaven tables to Iceberg, first create the table(s) you want to write. This example uses two tables:

```groovy docker-config=iceberg test-set=1 order=source2024,source2025
source2024 = emptyTable(100).update("Year = 2024", "X = i", "Y = 2 * X", "Z = randomDouble(-1, 1)")
source2025 = emptyTable(50).update("Year = 2024", "X = 100 + i", "Y = 3 * X", "Z = randomDouble(-100, 100)")
```

Writing multiple Deephaven tables to the same Iceberg table _requires_ that the tables have the same definition, regardless of whether or not the Iceberg table is partitioned.

#### Unpartitioned Iceberg tables

When writing data to an unpartitioned Iceberg table, you need the Deephaven table definition:

```groovy docker-config=iceberg test-set=1 order=null
sourceDef = source2024.getDefinition()
```

Then, create an [`IcebergTableAdapter`](/core/javadoc/io/deephaven/iceberg/util/IcebergTableAdapter.html) from the `source2024` table's definition, and a table identifier, which must include the Iceberg namespace (`nyc`):

```groovy docker-config=iceberg test-set=1 order=null
sourceAdapter = restAdapter.createTable("nyc.source", sourceDef)
```

To write the table to Iceberg, you'll need to create an [`IcebergTableWriter`](/core/javadoc/io/deephaven/iceberg/util/IcebergTableWriter.html). A single writer instance with a fixed table definition can write as many Deephaven tables as desired, given that all tables have the same definition as provided to the writer. Most of the heavy lifting is done when the writer is created, so it's more efficient to create a writer once and write many tables than to create a writer for each table.

To create a writer instance, you need to define the [`TableParquetWriterOptions`](/core/javadoc/io/deephaven/iceberg/util/TableParquetWriterOptions.html) to configure the writer:

```groovy docker-config=iceberg test-set=1 order=null
import io.deephaven.extensions.s3.*
import org.apache.iceberg.catalog.*

// Define the writer options
writerOptions = TableParquetWriterOptions.builder()
    .tableDefinition(sourceDef)
    .build()

// Create the writer
sourceWriter = sourceAdapter.tableWriter(writerOptions)
```

Now you can write the data to Iceberg. The following code block writes the `source2024` and `source2025` tables to the Iceberg table `nyc.source`:

```groovy docker-config=iceberg test-set=1 order=null
sourceWriter.append(IcebergWriteInstructions.builder()
    .addTables(source2024, source2025).build())
```

#### Partitioned Iceberg tables

To write data to a partitioned Iceberg table, you must specify one or more partitioning columns in the [`TableDefinition`](/core/javadoc/io/deephaven/engine/table/TableDefinition.html):

```groovy docker-config=iceberg test-set=1 order=null
import io.deephaven.engine.table.ColumnDefinition
import io.deephaven.engine.table.TableDefinition

sourceDefPartitioned = TableDefinition.of(
    ColumnDefinition.ofInt("Year").withPartitioning(),
    ColumnDefinition.ofInt("X"),
    ColumnDefinition.ofInt("Y"),
    ColumnDefinition.ofDouble("Z")
)
```

First, create an [`IcebergTableAdapter`](/core/javadoc/io/deephaven/iceberg/util/IcebergTableAdapter.html) from the `source` table's definition, and a table identifier, which must include the Iceberg namespace (`nyc`):

```groovy docker-config=iceberg test-set=1 order=null
sourceAdapterPartitioned = restAdapter.createTable("nyc.sourcePartitioned", sourceDefPartitioned)
```

To write the table to Iceberg, you'll need to create an [`IcebergTableWriter`](/core/javadoc/io/deephaven/iceberg/util/IcebergTableWriter.html). A single writer instance with a fixed table definition can write as many Deephaven tables as desired, given that all tables have the same definition as provided to the writer. Most of the heavy lifting is done when the writer is created, so it's more efficient to create a writer once and write many tables than to create a writer for each table.

To create a writer instance, you need to define the [`TableParquetWriterOptions`](/core/javadoc/io/deephaven/iceberg/util/TableParquetWriterOptions.html) to configure the writer:

```groovy docker-config=iceberg test-set=1 order=null
import io.deephaven.extensions.s3.*
import org.apache.iceberg.catalog.*

// Define the writer options
writerOptionsPartitioned = TableParquetWriterOptions.builder()
    .tableDefinition(sourceDefPartitioned)
    .build()

// Create the writer
sourceWriterPartitioned = sourceAdapterPartitioned.tableWriter(writerOptionsPartitioned)
```

Now you can write the data to Iceberg. The following code block writes the `source_2024` and `source_2025` tables to the `nyc.source_partitioned` table. The partition paths are specified in the [`IcebergWriteInstructions`](/core/javadoc/io/deephaven/iceberg/util/IcebergWriteInstructions.Builder.html):

```groovy docker-config=iceberg test-set=1 order=null
sourceWriterPartitioned.append(IcebergWriteInstructions.builder()
    .addTables(source2024.dropColumns("Year"), source2025.dropColumns("Year"))
    .addPartitionPaths("Year=2024", "Year=2025")
    .build())
```

> [!NOTE]
> The partitioning column(s) cannot be written to Iceberg, as they are already specified in the partition path. The above example drops them from the Deephaven tables before writing.

#### Check the write operations

Deephaven currently only supports appending data to Iceberg tables. Each append operation creates a new snapshot. When multiple tables are written in a single `append` call, all tables are written in the same snapshot.

Similarly, you can also write to a partitioned Iceberg table by providing the exact partition path where each Deephaven table should be appended. See [`IcebergWriteInstructions`](/core/javadoc/io/deephaven/iceberg/util/IcebergWriteInstructions.html) for more information.

Check that the operations worked by reading the Iceberg tables back into Deephaven using the same table adapter:

```groovy docker-config=iceberg test-set=1 order=sourceFromIceberg,sourcePartitionedFromIceberg
sourceFromIceberg = sourceAdapter.table()
sourcePartitionedFromIceberg = sourceAdapterPartitioned.table()
```

### Custom Iceberg instructions

You can set custom instructions when reading from or writing to Iceberg in Deephaven. The following sections deal with different custom instructions you can set.

#### Refreshing Iceberg tables

Deephaven also supports reading refreshing Iceberg tables. The [`IcebergUpdateMode`](/core/javadoc/io/deephaven/iceberg/util/IcebergUpdateMode.html) class has three different supported update modes:

- Static
- Refreshed manually
- Refreshed automatically

The examples above cover the static case. The following code block creates update modes for manually and automatically refreshing Iceberg tables. For automatically refreshing tables, the refresh interval can be set as an integer number of milliseconds. If no interval is set, the default is once per minute.

```groovy order=null
import io.deephaven.iceberg.util.IcebergUpdateMode

// Manually refreshing
manualRefresh = IcebergUpdateMode.manualRefreshingMode()

// Automatically refreshing every minute
autoRefreshEveryMinute = IcebergUpdateMode.autoRefreshingMode()

// Automatically refreshing every 30 seconds
autoRefreshEvery30Seconds = IcebergUpdateMode.autoRefreshingMode(30_000)
```

#### Table definition

You can specify the resultant table definition when building [`IcebergReadInstructions`](/core/javadoc/io/deephaven/iceberg/util/IcebergReadInstructions.html). This is useful when Deephaven cannot automatically infer the correct data types for an Iceberg table. The following code block defines a custom table definition to use when reading from Iceberg:

```groovy order=null
import io.deephaven.iceberg.util.IcebergReadInstructions
import io.deephaven.engine.table.ColumnDefinition
import io.deephaven.engine.table.TableDefinition

defInstructions = IcebergReadInstructions.builder()
    .tableDefinition(
        TableDefinition.of(
            ColumnDefinition.ofLong("ID"),
            ColumnDefinition.ofTime("Timestamp"),
            ColumnDefinition.ofString("Operation"),
            ColumnDefinition.ofString("Summary")
        )
    )
    .build()
```

#### Column renames

You can rename columns when reading from Iceberg as well:

```groovy order=null
import io.deephaven.iceberg.util.IcebergReadInstructions

icebergInstructionsRenames = IcebergReadInstructions.builder()
    .putAllColumnRenames(
        Map.of(
            "tpep_pickup_datetime", "PickupTime",
            "tpep_dropoff_datetime", "DropoffTime",
            "passenger_count", "NumPassengers",
            "trip_distance", "Distance",
        )
    )
```

#### Snapshot ID

You can tell Deephaven to read a specific snapshot of an Iceberg table based on its snapshot ID:

```groovy order=null
import io.deephaven.iceberg.util.IcebergReadInstructions

snapshotInstructions = IcebergReadInstructions.builder()
    .snapshotId(1234567890L)
    .build()
```

## Next steps

This guide presented a basic example of reading from and writing to an Iceberg catalog in Deephaven. These examples can be extended to include other catalog types, more complex queries, catalogs with multiple namespaces, snapshots, custom instructions, and more.

## Related documentation

- [Javadoc](/core/javadoc/io/deephaven/iceberg/util/package-summary.html)
