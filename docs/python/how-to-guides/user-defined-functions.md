---
title: User-defined functions
---

<!-- TODO: Do we need to cover how to use class methods -->

User-defined functions in Python can be used in Deephaven queries in several ways. This guide explains when and how they are used in Deephaven.

In Python, a user-defined function is defined using the `def` keyword. User-defined functions can take an arbitrary number of input parameters, and have any arbitrary amount of return values. The function `f` below returns the sum of its two input values.

```python test-set=1
def f(a, b):
    return a + b
```

## User-defined functions in table operations

User-defined functions can be used in query strings just like any method would. The query below uses the function `f` in a table operation.

```python test-set=1
from deephaven import empty_table

source = empty_table(10).update(["X = i", "Y = 2 * i", "Z = f(X, Y)"])
```

When using user-defined Python functions in table operations, there are several factors to consider. These are explored in each subsection below.

### Returned data type

The Deephaven query engine is implemented largely in Java. Since these are Python functions, that means that Java is calling Python code. Unless the query engine is given some information about the returned data type, it will store the return value as a Java [`Object`](https://docs.oracle.com/en/java/javase/11/docs/api/index.html) or a [jpy](./use-jpy.md) [`PyObject`](https://jpy.readthedocs.io/en/0.7.2/_static/java-apidocs/org/jpy/PyObject.html).

Look at the output type of the function `f`, called [above](#user-defined-functions-in-table-operations).

```python test-set=1
source_meta = source.meta_table
```

[`Object`](https://docs.oracle.com/en/java/javase/11/docs/api/index.html) columns and [`PyObject`](https://jpy.readthedocs.io/en/0.7.2/_static/java-apidocs/org/jpy/PyObject.html) columns are incompatible with a large number of operations and are generally slow.

There are two ways to avoid these column types: type hints and typecasts.

> [!NOTE]
> See [data types in Python and Java](../conceptual/data-types.md) for more information on this topic.

#### Type hints

[Type hints](https://peps.python.org/pep-0484/) in Python tell the compiler what data types to expect as input and output. Type hints in input arguments are denoted by the colon character (`:`) after the variable name. For function outputs, they are denoted by the use of `->`. The following example uses type hints in the function `f`, which causes the [`update`](../reference/table-operations/select/update.md) operation that calls it to return a proper data type.

```python test-set=1 order=source_meta,source
from deephaven import empty_table


def f(a: int, b: int) -> int:
    return a + b


source = empty_table(10).update(["X = i", "Y = 2 * i", "Z = f(X, Y)"])
source_meta = source.meta_table
```

Deephaven supports the following data types in type hints:

- Python `bool` and NumPy `np.bool_` to Java primitive `boolean`
- Python `int` and NumPy `np.int_` to Java primitive `long`
- Python `float` and NumPy `np.double` to Java primitive `double`
- NumPy `np.intc` to Java primitive `int`
- NumPy `np.single` to Java primitive `float`
- NumPy `np.byte` to Java primitive `byte`
- NumPy `np.short` to Java primitive `short`
- Python `str` and NumPy `np.str_` to Java `java.lang.String`
- Python `datetime.datetime`, Pandas `pd.Timestamp`, and `np.datetime64` to Java `java.time.Instant`
- Python `Sequence` and NumPy `ndarray` to Java arrays
- Python objects to Java `java.lang.Objects`

#### Typecasts

Alternatively, a typecast can be used in place of a type hint. A typecast precedes a function call in the query language using parentheses that enclose the data type:

```python test-set= order=source_meta,source
from deephaven import empty_table


def f(a, b):
    return a + b


source = empty_table(10).update(["X = i", "Y = 2 * i", "Z = (int)f(X, Y)"])
source_meta = source.meta_table
```

### Query language methods

The second thing to consider when calling a user-defined function from the query language is:

- Can I perform the same operation using a built-in query language method?

For instance, the function `sine` is called in the query below:

```python test-set=2
from deephaven import empty_table
import numpy as np


def sine(x):
    return np.sin(x)


source = empty_table(100).update(["X = 0.1 * i", "Y = sine(X)"])
```

Deephaven has a built-in [`sin`](https://deephaven.io/core/javadoc/io/deephaven/function/Numeric.html#sin(double)) method that can be used from the query language with no imports. It will be faster than any Python method in a table operation, and will return the correct data type.

```python test-set=2
from deephaven import empty_table

source = empty_table(100).update(["X = 0.1 * i", "Y = sin(X)"])
```

Deephaven has a large number of built-in methods that can be called from query strings without any imports or classpaths. For more information and a complete list of what's available, see [auto-imported functions in Deephaven](../reference/query-language/query-library/auto-imported-functions.md).

### Multiple return values

Functions in Python can return an arbitrary number of values. The following function returns both the sum and the difference of the two input values.

```python test-set=1
def g(a, b):
    return a + b, a - b
```

While this function can be called from Python without any problem, it will _not_ work in a Deephaven table operation. The query engine is implemented largely in Java, which does not automatically unpack output values from functions.

> [!WARNING]
> Do not call functions with multiple outputs in table operations.

Instead of returning multiple values from a Python function, wrap the returned values in a single iterable such as a [list](https://docs.python.org/3/library/stdtypes.html#sequence-types-list-tuple-range) or a [NumPy array](https://numpy.org/doc/stable/reference/generated/numpy.array.html) and unpack them manually in a query string.

## Passing tables to functions

Python functions can take tables as input and return tables as output. Some Deephaven operations specifically require this usage pattern.

The following example uses the function `do_agg` to perform [multiple aggregations](./combined-aggregations.md) on a table.

```python order=result,source
from deephaven.agg import sum_, avg
from deephaven import empty_table

source = empty_table(20).update(
    ["Letter = (i % 2 == 0) ? `A` : `B`", "X = i", "Y = randomDouble(0.0, 10.0)"]
)


def do_agg(t):
    return t.agg_by(
        aggs=[sum_(cols=["SumX = X", "SumY = Y"]), avg(cols=["AvgX = X", "AvgY = Y"])],
        by="Letter",
    )


result = do_agg(source)
```

> [!NOTE]
> Deephaven tables are immutable. A function that modifies a table will have no effect unless a table is returned from it.

### Partitioned tables

Some [partitioned table operations](./partitioned-tables.md) require the use of user-defined Python functions that modify tables:

- [`transform`](../reference/table-operations/partitioned-tables/transform.md)
- [`partitioned_transform`](../reference/table-operations/partitioned-tables/partitioned-transform.md)

Partitioned tables are beyond the scope of this guide. See the links above for more information on user-defined functions and partitioned tables.

## Import modules with user-defined functions

It's common in Python to use custom user-defined functions from a separate Python file or module. There are two ways to import these files:

- Use [`sys.path.append`](https://docs.python.org/3/library/sys.html#sys.path).
- Use [absolute or relative imports](https://docs.python.org/2/whatsnew/2.5.html#pep-328-absolute-and-relative-imports).

> [!NOTE]
> When running [Deephaven from Docker](../getting-started/docker-install.md), the path must be visible inside of the Docker container. For more information, see [Docker volumes](../conceptual/docker-data-volumes.md).

The following example appends `/data/storage/modules` to [`sys.path`](https://docs.python.org/3/library/sys_path_init.html) so that Python files in it can be imported.

```python skip-test
import sys

sys.path.append("/data/storage/modules")
```

## Related documentation

- [Data types](../conceptual/data-types.md)
- [Docker volumes](../conceptual/docker-data-volumes.md)
- [Auto-imported functions](../reference/query-language/query-library/auto-imported-functions.md)
