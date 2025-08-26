---
title: Other rolling utilities with update_by
sidebar_label: Other rolling utilities
---

This guide covers general rolling operations in the [`updateby`](/core/pydoc/code/deephaven.updateby.html#module-deephaven.updateby) Python module. To learn about cumulative, rolling, and moving statistics, see our [related guide](../how-to-guides/rolling-calculations.md).

## Rolling formulas

The [`updateby`](/core/pydoc/code/deephaven.updateby.html#module-deephaven.updateby) module enables users to create custom rolling aggregations with the [`rolling_formula_tick`](../reference/table-operations/update-by-operations/rolling-formula-tick.md) and [`rolling_formula_time`](../reference/table-operations/update-by-operations/rolling-formula-time.md) functions. For more information on tick vs. time operations, see [this section](../how-to-guides/rolling-calculations.md#time-based-vs-tick-based-operations) of the rolling statistics guide.

The user-defined formula can utilize any of Deephaven's [built-in functions](../reference/query-language/query-library/auto-imported-functions.md), [arithmetic operators](../how-to-guides/formulas-how-to.md#arithmetic-operators), or even [user-defined Python functions](../how-to-guides/user-defined-functions.md).

### `rolling_formula_tick`

Use [`rolling_formula_tick`](../reference/table-operations/update-by-operations/rolling-formula-tick.md) to create custom tick-based rolling formulas. Here's an example that computes the rolling geometric mean of a column `X` by group:

```python order=result
from deephaven import empty_table
import deephaven.updateby as uby

source = empty_table(100).update(
    ["Letter = (i % 2 == 0) ? `A` : `B`", "X = randomInt(0, 100)"]
)

result = source.update_by(
    uby.rolling_formula_tick(
        formula="pow(product(x), 1/count(x))",
        formula_param="x",
        cols="GeomMeanX=X",
        rev_ticks=3,
    ),
    by="Letter",
)
```

### `rolling_formula_time`

To create custom time-based rolling formulas, use [`rolling_formula_time`](../reference/table-operations/update-by-operations/rolling-formula-time.md). You must supply a timestamp column, and can specify the time window as backward-looking, forward-looking, or both. Here's an example that computes the 5-second rolling geometric mean of a column `X` by group:

```python order=result
from deephaven import empty_table
import deephaven.updateby as uby

source = empty_table(100).update(
    [
        "Timestamp = '2023-01-01T00:00:00 ET' + i * SECOND",
        "Letter = (i % 2 == 0) ? `A` : `B`",
        "X = randomInt(0, 25)",
    ]
)

result = source.update_by(
    uby.rolling_formula_time(
        ts_col="Timestamp",
        formula="pow(product(x), 1/count(x))",
        formula_param="x",
        cols="GeomMeanX=X",
        rev_time="PT5s",
    ),
    by="Letter",
)
```

## Rolling groups

In addition to custom rolling formulas, [`updateby`](/core/pydoc/code/deephaven.updateby.html#module-deephaven.updateby) provides the ability to create rolling groups with [`rolling_group_tick`](../reference/table-operations/update-by-operations/rolling-group-tick.md) and [`rolling_group_time`](../reference/table-operations/update-by-operations/rolling-group-time.md). The grouped data are represented as arrays. See the guide on [how to work with arrays](../how-to-guides/work-with-arrays.md) for more details.

### `rolling_group_tick`

Use [`rolling_group_tick`](../reference/table-operations/update-by-operations/rolling-group-tick.md) to create tick-based rolling groups, where each group will have a specified number of entries determined by `rev_ticks` and `fwd_ticks`. Here's an example that creates rolling groups with the three previous rows and the current row:

```python order=result
from deephaven import empty_table
import deephaven.updateby as uby

source = empty_table(100).update(
    ["Letter = randomBool() ? `A` : `B`", "X = randomInt(0, 100)"]
)

result = source.update_by(
    uby.rolling_group_tick(cols="GroupX=X", rev_ticks=4), by="Letter"
)
```

To create groups that include data after the current row, use the `fwd_ticks` parameter. This example creates a group that consists of the two previous rows, the current row, and the next four rows:

```python order=result
from deephaven import empty_table
import deephaven.updateby as uby

source = empty_table(100).update(
    ["Letter = randomBool() ? `A` : `B`", "X = randomInt(0, 100)"]
)

result = source.update_by(
    uby.rolling_group_tick(cols="GroupX=X", rev_ticks=3, fwd_ticks=4), by="Letter"
)
```

### `rolling_group_time`

Similarly, use [`rolling_group_time`](../reference/table-operations/update-by-operations/rolling-group-time.md) to create time-based rolling groups:

```python order=result
from deephaven import empty_table
import deephaven.updateby as uby

source = empty_table(100).update(
    [
        "Timestamp = '2023-01-01T00:00:00 ET' + i * SECOND",
        "Letter = randomBool() ? `A` : `B`",
        "X = randomInt(0, 25)",
    ]
)

result = source.update_by(
    uby.rolling_group_time(ts_col="Timestamp", cols="GroupX=X", rev_time="PT3s"),
    by="Letter",
)
```

These groups are timestamp-based, so they are not guaranteed to contain elements from any previous row. This is in contrast to [`rolling_group_tick`](../reference/table-operations/update-by-operations/rolling-group-tick.md), which always yields groups of a fixed size after that size has been reached.

The `fwd_time` parameter is used to create groups that include rows occuring after the current row. Here's an example that creates rolling groups out of every row within five seconds of the current row:

```python order=result
from deephaven import empty_table
import deephaven.updateby as uby

source = empty_table(100).update(
    [
        "Timestamp = '2023-01-01T00:00:00 ET' + i * SECOND",
        "Letter = randomBool() ? `A` : `B`",
        "X = randomInt(0, 25)",
    ]
)

result = source.update_by(
    uby.rolling_group_time(
        ts_col="Timestamp", cols="GroupX=X", rev_time="PT5s", fwd_time="PT5s"
    ),
    by="Letter",
)
```

## Sequential differences with `delta`

Deephaven's [`delta`](../reference/table-operations/update-by-operations/delta.md) function can be used to compute sequential differences in a column of a table. Here's a simple example:

```python order=result
from deephaven import empty_table
import deephaven.updateby as uby

source = empty_table(100).update(
    [
        "Timestamp = '2023-01-01T00:00:00 ET' + i * SECOND",
        "X = randomInt(0, 25)",
    ]
)

result = source.update_by(uby.delta(cols="DiffX=X"))
```

Like all other [`updateby`](/core/pydoc/code/deephaven.updateby.html#module-deephaven.updateby) functions, the `by` argument is used to specify grouping columns, so that sequential differences can be computed on a per-group basis:

```python order=result
from deephaven import empty_table
import deephaven.updateby as uby

source = empty_table(100).update(
    [
        "Timestamp = '2023-01-01T00:00:00 ET' + i * SECOND",
        "Letter = randomBool() ? `A` : `B`",
        "X = randomInt(0, 25)",
    ]
)

result = source.update_by(uby.delta(cols="DiffX=X"), by="Letter")
```

### Detrend time-series data

Sequential differencing is often used as a first measure for detrending time-series data. The [`updateby`](/core/pydoc/code/deephaven.updateby.html#module-deephaven.updateby) module provides the [`delta`](../reference/table-operations/update-by-operations/delta.md) function to make this easy:

```python order=no_detrend,detrend,source,result
from deephaven import empty_table
import deephaven.updateby as uby
from deephaven.plot import Figure

source = empty_table(1000).update(
    [
        "Timestamp='2023-01-13T12:00 ET' + i*MINUTE",
        "Ticker = i%2==0 ? `ABC` : `XYZ`",
        "Price = i%2==0 ? 100*sin(i/40)+100*random() : 100*cos(i/40)+100*random()+i/2",
    ]
)

result = source.update_by(uby.delta("DiffPrice=Price"), by="Ticker")

no_detrend = (
    Figure()
    .plot_xy(
        series_name="ABC", t=result.where("Ticker == `ABC`"), x="Timestamp", y="Price"
    )
    .plot_xy(
        series_name="XYZ", t=result.where("Ticker == `XYZ`"), x="Timestamp", y="Price"
    )
    .show()
)

detrend = (
    Figure()
    .plot_xy(
        series_name="ABC",
        t=result.where("Ticker == `ABC`"),
        x="Timestamp",
        y="DiffPrice",
    )
    .plot_xy(
        series_name="XYZ",
        t=result.where("Ticker == `XYZ`"),
        x="Timestamp",
        y="DiffPrice",
    )
    .show()
)
```

### Handle nulls with `DeltaControl`

The [`delta`](../reference/table-operations/update-by-operations/delta.md) function takes an optional argument `delta_control` that is used to determine how null values are treated. To use this argument, you must supply a [`DeltaControl`](../reference/table-operations/update-by-operations/DeltaControl.md) instance. The following behaviors are available via [`DeltaControl`](../reference/table-operations/update-by-operations/DeltaControl.md):

- `DeltaControl.NULL_DOMINATES`: A valid value following a null value returns null.
- `DeltaControl.VALUE_DOMINATES`: A valid value following a null value returns the valid value.
- `DeltaControl.ZERO_DOMINATES`: A valid value following a null value returns zero.

To see how each of these behave in context, consider the following example:

```python order=result
from deephaven import empty_table
import deephaven.updateby as uby

source = empty_table(100).update(
    [
        "Timestamp = '2023-01-01T00:00:00 ET' + i * SECOND",
        "Letter = randomBool() ? `A` : `B`",
        "X = randomInt(0, 25)",
    ]
)

result = source.update_by(
    [
        uby.delta(cols="DefaultDeltaX=X"),
        uby.delta(
            cols="NullDomDeltaX=X",
            delta_control=uby.DeltaControl(uby.DeltaControl.NULL_DOMINATES),
        ),
        uby.delta(
            cols="ValueDomDeltaX=X",
            delta_control=uby.DeltaControl(uby.DeltaControl.VALUE_DOMINATES),
        ),
        uby.delta(
            cols="ZeroDomDeltaX=X",
            delta_control=uby.DeltaControl(uby.DeltaControl.ZERO_DOMINATES),
        ),
    ],
    by="Letter",
)
```

By default, [`delta`](../reference/table-operations/update-by-operations/delta.md) uses `NULL_DOMINATES`, so differencing a number from a null will always return a null.

## Handle nulls with `forward_fill`

The [`updateby`](/core/pydoc/code/deephaven.updateby.html#module-deephaven.updateby) module provides the [`forward_fill`](../reference/table-operations/update-by-operations/forward-fill.md) function to help deal with null data values. It fills in null values with the most recent non-null values, and like all [`updateby`](/core/pydoc/code/deephaven.updateby.html#module-deephaven.updateby) operations, can do so on a per-group basis.

Here's an example of using [`forward_fill`](../reference/table-operations/update-by-operations/forward-fill.md) to fill up null values by group:

```python order=result
from deephaven import empty_table
import deephaven.updateby as uby

source = empty_table(100).update(
    [
        "Timestamp = '2023-01-01T00:00:00 ET' + i * SECOND",
        "Letter = randomBool() ? `A` : `B`",
        "X = randomBool() ? NULL_INT : randomInt(0, 25)",
    ]
)

result = source.update_by(uby.forward_fill("FillX=X"), by="Letter")
```

## Related documentation

- [Create a time table](./time-table.md)
- [Create a new or empty table](./new-and-empty-table.md)
- [How to create XY series plots](./plotting/api-plotting.md#xy-series)
- [How to use select, view, and update](./use-select-view-update.md)
- [How to use update_by](./use-update-by.md)
- [Handle nulls, infs, and NaNs](./handle-null-inf-nan.md)
- [User-Defined Functions](../how-to-guides/user-defined-functions.md)
- [`rolling_formula_tick`](../reference/table-operations/update-by-operations/rolling-formula-tick.md)
- [`rolling_formula_time`](../reference/table-operations/update-by-operations/rolling-formula-time.md)
- [`rolling_group_tick`](../reference/table-operations/update-by-operations/rolling-group-tick.md)
- [`rolling_group_time`](../reference/table-operations/update-by-operations/rolling-group-time.md)
- [`delta`](../reference/table-operations/update-by-operations/delta.md)
- [`forward_fill`](../reference/table-operations/update-by-operations/forward-fill.md)
