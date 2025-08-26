---
title: IcebergTable
---

The `IcebergTable` class is a subclass of Deephaven table that allows users to dynamically update the table with new snapshots from an Iceberg catalog.

## Constructors

- [`IcebergTableAdapter.table`](./iceberg-table-adapter.md#methods)

## Methods

- [`update`](/core/pydoc/code/deephaven.experimental.iceberg.html#deephaven.experimental.iceberg.IcebergTable.update): Updates the table to match the contents of the specified snapshot ID. This is only useable if the update mode is set to [`IcebergUpdateMode.manual_refresh`](./iceberg-update-mode.md#methods). If no snapshot ID is given, the most recent snapshot is used.

## Examples

The following code block.

```python skip-test
from deephaven.experimental import iceberg

local_adapter = iceberg.adapter_s3_rest(
    name="minio-iceberg",
    catalog_uri="http://rest:8181",
    warehouse_location="s3a://warehouse/wh",
    region_name="us-east-1",
    access_key_id="admin",
    secret_access_key="password",
    end_point_override="http://minio:9000",
)

manual_refresh_instructions = iceberg.IcebergReadInstructions(
    update_mode=iceberg.IcebergUpdateMode.manual_refresh()
)
taxis = iceberg_taxis.table(manual_refresh_instructions)

# Some time later, refresh the Iceberg table
taxis = taxis.update()
```

## Related documentation

- [`adapter`](./adapter.md)
- [`adapter_aws_glue`](./adapter-aws-glue.md)
- [`adapter_s3_rest`](./adapter-s3-rest.md)
- [`IcebergCatalogAdapter`](./iceberg-catalog-adapter.md)
- [`IcebergReadInstructions`](./iceberg-read-instructions.md)
- [`IcebergTableAdapter`](./iceberg-table-adapter.md)
- [`IcebergTableWriter`](./iceberg-table-writer.md)
- [`IcebergUpdateMode`](./iceberg-update-mode.md)
- [`IcebergWriteInstructions`](./iceberg-write-instructions.md)
- [`SortOrderProvider`](./sort-order-provider.md)
- [`TableParquetWriterOptions`](./table-parquet-writer-options.md)
- [Pydoc](/core/pydoc/code/deephaven.experimental.iceberg.html#deephaven.experimental.iceberg.IcebergTable)
