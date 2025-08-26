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

## Deephaven's Iceberg module

Deephaven's Iceberg integration is provided by the [`deephaven.experimental.iceberg`](/core/pydoc/code/deephaven.experimental.iceberg.html#module-deephaven.experimental.iceberg) module. The module contains the following classes and functions:

- Classes:
  - [`IcebergCatalogAdapter`](../../reference/iceberg/iceberg-catalog-adapter.md)
  - [`IcebergReadInstructions`](../../reference/iceberg/iceberg-read-instructions.md)
  - [`IcebergTable`](../../reference/iceberg/iceberg-table.md)
  - [`IcebergTableAdapter`](../../reference/iceberg/iceberg-table-adapter.md)
  - [`IcebergUpdateMode`](../../reference/iceberg/iceberg-update-mode.md)
  - [`IcebergWriteInstructions`](../../reference/iceberg/iceberg-write-instructions.md)
  - [`IcebergTableWriter`](../../reference/iceberg/iceberg-table-writer.md)
  - [`InferenceResolver`](../../reference/iceberg/inference-resolver.md)
  - [`SchemaProvider`](../../reference/iceberg/schema-provider.md)
  - [`SortOrderProvider`](../../reference/iceberg/sort-order-provider.md)
  - [`TableParquetWriterOptions`](../../reference/iceberg/table-parquet-writer-options.md)
  - [`UnboundResolver`](../../reference/iceberg/unbound-resolver.md)
- Methods:
  - [`adapter`](../../reference/iceberg/adapter.md)
  - [`adapter_aws_glue`](../../reference/iceberg/adapter-aws-glue.md)
  - [`adapter_s3_rest`](../../reference/iceberg/adapter-s3-rest.md)

When querying Iceberg tables located in any S3-compatible storage service, the [`deephaven.experimental.s3`](/core/pydoc/code/deephaven.experimental.s3.html#module-deephaven.experimental.s3) module must be used to read the data.

## A Deephaven deployment for Iceberg

The examples presented in this guide pull Iceberg data from a REST catalog. This section closely follows Iceberg's [Spark quickstart](https://iceberg.apache.org/spark-quickstart/). It extends the `docker-compose.yml` file in that guide to include Deephaven as part of the Iceberg Docker network. The Deephaven server starts alongside a Spark server, Iceberg REST API, and MinIO object store.

<details>
<summary>docker-compose.yml</summary>

```yaml
services:
  spark-iceberg:
    image: tabulario/spark-iceberg
    container_name: spark-iceberg
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
      until (/usr/bin/mc alias set minio http://minio:9000 admin password) do echo '...waiting...' && sleep 1; done;
      /usr/bin/mc mb minio/warehouse;
      /usr/bin/mc policy set public minio/warehouse;
      tail -f /dev/null
      "
  deephaven:
    image: ghcr.io/deephaven/server:latest
    networks:
      iceberg_net:
    ports:
      - "${DEEPHAVEN_PORT:-10000}:10000"
    environment:
      - START_OPTS=-Dauthentication.psk=YOUR_PASSWORD_HERE
    volumes:
      - ./data:/data
networks:
  iceberg_net:
```

</details>

> [!IMPORTANT]
> The `docker-compose.yml` file above sets the pre-shared key to `YOUR_PASSWORD_HERE`. This doesn't meet security best practices and should be changed in a production environment. For more information, see [pre-shared key authentication](../authentication/auth-psk.md).

Run `docker compose up` from the directory with the `docker-compose.yml` file. This starts the Deephaven server, Spark server, Iceberg REST API, and MinIO object store. When you're done, a `ctrl+C` or `docker compose down` stops the containers.

### Create an Iceberg catalog

This section follows the Iceberg [Spark quickstart](https://iceberg.apache.org/spark-quickstart/) by creating an Iceberg catalog with a single table and snapshot using the Iceberg REST API in Jupyter. The [docker-compose.yml](#a-deephaven-deployment-for-iceberg) extends the one in the Spark quickstart guide to include Deephaven as a service in the Iceberg Docker network. As such, the file starts up the following services:

- MinIO object store
- MinIO client
- Iceberg Spark server, reachable by Jupyter
- Deephaven server

Once the Docker containers are up and running, head to `http://localhost:8888` to access the Iceberg Spark server in Jupyter. Open either the `Iceberg - Getting Started` or `PyIceberg - Getting Started` notebooks, which create a catalog using the Iceberg REST API. The first four code blocks create an Iceberg table called `nyc.taxis`. Run this code to follow along with this guide, which uses the table in the sections below. All code blocks afterward are optional for our purposes.

## Interact with the Iceberg catalog

After creating the Iceberg catalog and table, head to the Deephaven IDE at `http://localhost:10000/ide`.

To interact with an Iceberg catalog, you must first create an instance of the [`IcebergCatalogAdapter`](../../reference/iceberg/iceberg-catalog-adapter.md) class. Since this guide uses a REST catalog, the adapter can be created using the more generic [`adapter`](../../reference/iceberg/adapter.md) method:

```python docker-config=iceberg test-set=1 order=null
from deephaven.experimental import iceberg

rest_adapter = iceberg.adapter(
    name="generic-adapter",
    properties={
        "type": "rest",
        "uri": "http://rest:8181",
        "client.region": "us-east-1",
        "s3.access-key-id": "admin",
        "s3.secret-access-key": "password",
        "s3.endpoint": "http://minio:9000",
        "io-impl": "org.apache.iceberg.aws.s3.S3FileIO",
    },
)
```

If you are working with a REST catalog backed by S3 storage, you can use the more specific [`adapter_s3_rest`](../../reference/iceberg/adapter-s3-rest.md) method:

```python docker-config=iceberg test-set=1 order=null
from deephaven.experimental import iceberg

rest_adapter = iceberg.adapter_s3_rest(
    name="minio-iceberg",
    catalog_uri="http://rest:8181",
    warehouse_location="s3a://warehouse/wh",
    region_name="us-east-1",
    access_key_id="admin",
    secret_access_key="password",
    end_point_override="http://minio:9000",
)
```

Similarly, if you are working with an AWS Glue catalog, you can use the [`adapter_aws_glue`](../../reference/iceberg/adapter-aws-glue.md) method.

Once an [`IcebergCatalogAdapter`](../../reference/iceberg/iceberg-catalog-adapter.md) has been created, it can query the namespaces and tables in a catalog. The following code block gets the available top-level namespaces and tables in the `nyc` namespace.

```python docker-config=iceberg test-set=1 order=namespaces,tables
namespaces = rest_adapter.namespaces()
tables = rest_adapter.tables(namespace="nyc")
```

### Load an Iceberg table into Deephaven

At this point, you can load a table from the catalog with [`load_table`](../../reference/iceberg/iceberg-catalog-adapter.md#methods). This returns an [`IcebergTableAdapter`](../../reference/iceberg/iceberg-table-adapter.md) rather than a Deephaven table. The table adapter provides you with several methods to read from or write to the underlying Iceberg table.

```python docker-config=iceberg test-set=1 order=null
iceberg_taxis = rest_adapter.load_table(table_identifier="nyc.taxis")
```

Now that we have the table adapter and the instructions, we can read the table into a Deephaven table:

```python docker-config=iceberg test-set=1
taxis = iceberg_taxis.table(update_mode=iceberg.IcebergUpdateMode.static())
```

For greater control over the resultant Deephaven table and greater resilience to schema changes, use an [`UnboundResolver`](../../reference/iceberg/unbound-resolver.md) to map the Iceberg table schema to a Deephaven table definition. The following code shows how to do so using field IDs so that if the Iceberg schema changes, the mapping is still valid:

```python docker-config=iceberg test-set=1 order=taxis
from deephaven import dtypes as dht

taxis_def = {
    "VendorID": dht.int64,
    "PickupTime": dht.Instant,
    "DropoffTime": dht.Instant,
    "NumPassengers": dht.double,
    "TripDistance": dht.double,
    "RateCodeID": dht.double,
    "StoreAndFwdFlag": dht.string,
    "PickupLocationID": dht.int64,
    "DropoffLocationID": dht.int64,
    "PaymentType": dht.long,
    "FareAmount": dht.double,
    "Extra": dht.double,
    "MtaTax": dht.double,
    "Tip": dht.double,
    "Tolls": dht.double,
    "ImprovementSurcharge": dht.double,
    "TotalCost": dht.double,
    "CongestionSurcharge": dht.double,
    "AirportFee": dht.double,
}

column_instructions = {
    "VendorID": 1,
    "PickupTime": 2,
    "DropoffTime": 3,
    "NumPassengers": 4,
    "TripDistance": 5,
    "RateCodeID": 6,
    "StoreAndFwdFlag": 7,
    "PickupLocationID": 8,
    "DropoffLocationID": 9,
    "PaymentType": 10,
    "FareAmount": 11,
    "Extra": 12,
    "MtaTax": 13,
    "Tip": 14,
    "Tolls": 15,
    "ImprovementSurcharge": 16,
    "TotalCost": 17,
    "CongestionSurcharge": 18,
    "AirportFee": 19,
}

resolver_by_id = iceberg.UnboundResolver(
    table_definition=taxis_def, column_instructions=column_instructions
)

iceberg_taxis = rest_adapter.load_table(
    table_identifier="nyc.taxis",
    resolver=iceberg.UnboundResolver(taxis_def, column_instructions),
)

taxis = iceberg_taxis.table(update_mode=iceberg.IcebergUpdateMode.static())
```

### Write Deephaven tables to Iceberg

To write one or more Deephaven tables to Iceberg, first create the table(s) you want to write. This example uses two tables:

```python docker-config=iceberg test-set=1 order=source_2024,source_2025
from deephaven import empty_table

source_2024 = empty_table(100).update(
    ["Year = 2024", "X = i", "Y = 2 * X", "Z = randomDouble(-1, 1)"]
)
source_2025 = empty_table(50).update(
    ["Year = 2025", "X = 100 + i", "Y = 3 * X", "Z = randomDouble(-100, 100)"]
)
```

Writing multiple Deephaven tables to the same Iceberg table _requires_ that the tables have the same definition, regardless of whether or not the Iceberg table is partitioned.

#### Unpartitioned Iceberg tables

When writing data to an unpartitioned Iceberg table, you need the Deephaven table definition:

```python docker-config=iceberg test-set=1 order=null
source_def = source_2024.definition
```

Then, create an [`IcebergTableAdapter`](../../reference/iceberg/iceberg-table-adapter.md) from a table definition and table identifier, which must include the Iceberg namespace (`nyc`):

```python docker-config=iceberg test-set=1 order=null
source_adapter = rest_adapter.create_table(
    table_identifier="nyc.source", table_definition=source_def
)
```

To write the table to Iceberg, you need to create an [`IcebergTableWriter`](../../reference/iceberg/iceberg-table-writer.md). A single writer instance with a fixed table definition can write as many Deephaven tables as desired, given that all tables have the same definition as provided to the writer. Most of the heavy lifting is done when the writer is created, so it's more efficient to create a writer once and write many tables than to create a writer for each table.

To create a writer instance, you'll need to define the [`TableParquetWriterOptions`](../../reference/iceberg/table-parquet-writer-options.md) to configure the writer:

```python docker-config=iceberg test-set=1 order=null
from deephaven.experimental import iceberg

# Define the writer options
writer_options = iceberg.TableParquetWriterOptions(table_definition=source_def)

# Create the writer
source_writer = source_adapter.table_writer(writer_options=writer_options)
```

Now you can write the data to Iceberg. The following code block writes the `source_2024` and `source_2025` tables to the `nyc.source` table:

```python docker-config=iceberg test-set=1 order=null
source_writer.append(iceberg.IcebergWriteInstructions([source_2024, source_2025]))
```

#### Partitioned Iceberg tables

To write data to a partitioned Iceberg table, you must specify one or more partitioning columns with [`deephaven.column`](/core/pydoc/code/deephaven.column.html#module-deephaven.column):

```python docker-config=iceberg test-set=1 order=null
from deephaven.column import col_def, ColumnType
from deephaven import dtypes as dht

source_def_partitioned = [
    col_def("Year", dht.int32, column_type=ColumnType.PARTITIONING),
    col_def("X", dht.int32),
    col_def("Y", dht.int32),
    col_def("Z", dht.double),
]
```

Then, create an [`IcebergTableAdapter`](../../reference/iceberg/iceberg-table-adapter.md) from a table definition and table identifier, which must include the Iceberg namespace:

```python docker-config=iceberg test-set=1 order=null
source_adapter_partitioned = rest_adapter.create_table(
    table_identifier="nyc.source_partitioned", table_definition=source_def_partitioned
)
```

To write the table to Iceberg, you'll need to create an [`IcebergTableWriter`](../../reference/iceberg/iceberg-table-writer.md). A single writer instance with a fixed table definition can write as many Deephaven tables as desired if they all have the same definition as provided to the writer. Most of the heavy lifting is done when the writer is created, so it's more efficient to create a writer once and write many tables than to create a writer for each table.

To create a writer instance, you'll need to define the [`TableParquetWriterOptions`](../../reference/iceberg/table-parquet-writer-options.md) to configure the writer:

```python docker-config=iceberg test-set=1 order=null
from deephaven.experimental import iceberg

# Define the writer options
writer_options_partitioned = iceberg.TableParquetWriterOptions(
    table_definition=source_def_partitioned
)

# Create the writer
source_writer_partitioned = source_adapter_partitioned.table_writer(
    writer_options=writer_options_partitioned
)
```

Now you can write the data to Iceberg. The following code block writes the `source_2024` and `source_2025` tables to the `nyc.source_partitioned` table. The partition paths are specified in the [`IcebergWriteInstructions`](../../reference/iceberg/iceberg-write-instructions.md):

```python docker-config=iceberg test-set=1 order=null
source_writer_partitioned.append(
    iceberg.IcebergWriteInstructions(
        [source_2024.drop_columns("Year"), source_2025.drop_columns("Year")],
        partition_paths=["Year=2024", "Year=2025"],
    )
)
```

> [!NOTE]
> The partitioning column(s) cannot be written to Iceberg, as they are already specified in the partition path. The above example drops them from the Deephaven tables before writing.

#### Check the write operations

Deephaven currently only supports appending data to Iceberg tables. Each append operation creates a new snapshot. When multiple tables are written in a single `append` call, all tables are written in the same snapshot.

Similarly, you can also write to a partitioned Iceberg table by providing the exact partition path where each Deephaven table should be appended. See [`IcebergWriteInstructions`](../../reference/iceberg/iceberg-write-instructions.md) for more information.

Check that the operations worked by reading the Iceberg tables back into Deephaven using the same table adapter:

```python docker-config=iceberg test-set=1 order=source_from_iceberg,source_from_iceberg_partitioned
source_from_iceberg = source_adapter.table()
source_from_iceberg_partitioned = source_adapter_partitioned.table()
```

### Custom Iceberg instructions

You can specify custom instructions when creating an [`IcebergReadInstructions`](../../reference/iceberg/iceberg-read-instructions.md) instance. Each subsection below covers a different custom instruction that can be passed in when reading Iceberg tables.

#### Refreshing Iceberg tables

Deephaven also supports refreshing Iceberg tables. The [`IcebergUpdateMode`](../../reference/iceberg/iceberg-update-mode.md) class specifies three different supported update modes:

- Static
- Refreshed manually
- Refreshed automatically

This guide already looked at static Iceberg tables. For Iceberg tables that can be refreshed manually and automatically, the following code block creates an instance of each mode:

```python skip-test
manual_refresh_mode = iceberg.IcebergUpdateMode.manual_refresh()
auto_refresh_mode_60s = iceberg.IcebergUpdateMode.auto_refresh()
auto_refresh_mode_30s = iceberg.IcebergUpdateMode.auto_refresh(auto_refresh_ms=30000)

# Manually refreshing
manual_refresh_instructions = iceberg.IcebergReadInstructions(
    update_mode=manual_refresh_mode
)

# Automatically refreshing every minute
auto_refresh_instructions_60s = iceberg.IcebergReadInstructions(
    update_mode=auto_refresh_mode_60s
)

# Automatically refreshing every 30 seconds
auto_refresh_instructions_30s = iceberg.IcebergReadInstructions(
    update_mode=auto_refresh_mode_30s
)
```

#### Table definition

You can specify the resultant table definition when building [`IcebergReadInstructions`](../../reference/iceberg/iceberg-read-instructions.md). This is useful when Deephaven cannot automatically infer the correct data types for an Iceberg table. The following code block defines a custom table definition to use when reading from Iceberg:

```python order=null
from deephaven.experimental import iceberg
from deephaven import dtypes as dht

def_instructions = iceberg.IcebergReadInstructions(
    table_definition={
        "ID": dht.long,
        "Timestamp": dht.Instant,
        "Operation": dht.string,
        "Summary": dht.string,
    }
)
```

#### Column renames

You can rename columns when reading from Iceberg as well:

```python order=null
from deephaven.experimental import iceberg

iceberg_instructions_renames = iceberg.IcebergReadInstructions(
    column_renames={
        "tpep_pickup_datetime": "PickupTime",
        "tpep_dropoff_datetime": "DropoffTime",
        "passenger_count": "NumPassengers",
        "trip_distance": "Distance",
    },
)
```

#### Snapshot ID

You can tell Deephaven to read a specific snapshot of an Iceberg table based on its snapshot ID:

```python order=null
from deephaven.experimental import iceberg

snapshot_instructions = iceberg.IcebergReadInstructions(snapshot_id=6738371110677246500)
```

## Next steps

This guide presented a basic example of interacting with an Iceberg catalog in Deephaven. These examples can be extended to include more complex queries, catalogs with multiple namespaces, snapshots, custom instructions, and more.

## Related documentation

- [`adapter_aws_glue`](../../reference/iceberg/adapter-aws-glue.md)
- [`adapter_s3_rest`](../../reference/iceberg/adapter-s3-rest.md)
- [`IcebergCatalogAdapter`](../../reference/iceberg/iceberg-catalog-adapter.md)
- [`IcebergReadInstructions`](../../reference/iceberg/iceberg-read-instructions.md)
- [`IcebergTable`](../../reference/iceberg/iceberg-table.md)
- [`IcebergTableAdapter`](../../reference/iceberg/iceberg-table-adapter.md)
- [`IcebergTableWriter`](../../reference/iceberg/iceberg-table-writer.md)
- [`IcebergUpdateMode`](../../reference/iceberg/iceberg-update-mode.md)
- [Iceberg Pydoc](/core/pydoc/code/deephaven.experimental.iceberg.html)
- [s3 Pydoc](/core/pydoc/code/deephaven.experimental.s3.html)
