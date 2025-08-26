---
title: Work with strings
sidebar_label: Strings
---

[Strings](https://en.wikipedia.org/wiki/String_(computer_science)) are sequences of characters that form words, sentences, phrases, or statements in programming languages. Deephaven queries use strings ubiquitously. Understanding their uses is critical to becoming a strong Deephaven developer.

> [!NOTE]
> This guide assumes you are familiar with using strings in [Python](https://docs.python.org/3/library/string.html). If not, please refer to the [Python documentation](https://docs.python.org/3/library/stdtypes.html#text-sequence-type-str) for more information.

## Strings in table operations (query strings)

<!-- TODO: Update link to relational operators #3812 -->

The first and foremost place strings get used in Deephaven queries is table operations. The strings used as inputs for table operations are typically called query strings. A query string contains a [formula](../how-to-guides/formulas-how-to.md), which either assigns data to or filters a column by relating the column to its values through the use of one or more [operators](../how-to-guides/formulas-how-to.md#operators).

The following code passes the query string `X = i` to [`update`](../reference/table-operations/select/update.md).

```python
from deephaven import empty_table

source = empty_table(5).update("X = i")
```

`X = i` is a query string because it relates a column (`X`) to its values ([`i`](../reference/query-language/variables/special-variables.md)) by the [assignment operator](../how-to-guides/formulas-how-to.md#assignment-operators) `=`. Query strings either [create](./new-and-empty-table.md#column-types), [modify](./drop-move-rename-columns.md), or [filter](./use-filters.md) data from tables. The following query [creates](./new-and-empty-table.md) data and then [filters](./use-filters.md) it with query strings.

```python
from deephaven import empty_table

source = empty_table(10).update("X = i").where("X % 2 == 1")
```

Methods like [`update`](../reference/table-operations/select/update.md) and [`where`](../reference/table-operations/filter/where.md) can take a [list](https://docs.python.org/3/tutorial/datastructures.html) as input to pass in multiple query strings. The code below uses [`update`](../reference/table-operations/select/update.md) to add two new columns, `X` and `Y`, to an [empty table](../reference/table-operations/create/emptyTable.md).

```python
from deephaven import empty_table

source = empty_table(10).update(["X = i", "Y = String.valueOf(X)"])
```

### Query strings and f-strings

Python's [f-strings](https://docs.python.org/3/tutorial/inputoutput.html#formatted-string-literals) can be used to generate query strings in queries programmatically. This approach can increase the readability of queries by dramatically reducing the amount of code required. The query below creates two identical tables with 10 columns both with and without [f-strings](https://docs.python.org/3/tutorial/inputoutput.html#formatted-string-literals) to show the difference in the amount of code required.

```python order=source_fstring,source
from deephaven import empty_table

source_fstring = empty_table(10).update([f"X{idx} = {idx} * i" for idx in range(10)])

source = empty_table(10).update(
    [
        "X0 = 0 * i",
        "X1 = 1 * i",
        "X2 = 2 * i",
        "X3 = 3 * i",
        "X4 = 4 * i",
        "X5 = 5 * i",
        "X6 = 6 * i",
        "X7 = 7 * i",
        "X8 = 8 * i",
        "X9 = 9 * i",
    ]
)
```

### Strings in query strings

Formulas in query strings can contain strings of their own. Strings inside query strings are denoted by backticks (`` ` ``). The following code uses a query string that contains another string in the [`where`](../reference/table-operations/filter/where.md) operation.

```python order=result,source
from deephaven import new_table
from deephaven.column import string_col

source = new_table([string_col("X", ["A", "B", "B", "C", "B", "A", "B", "B", "C"])])

result = source.where(filters=["X = `C`"])
```

### String literals in query strings

Formulas in query strings can also make use of string literals. These are denoted by single quotes (`'`). String literals are different from normal [strings in query strings](#strings-in-query-strings) because they get interpreted differently. String literals can be inferred as another data type. The following example uses both a string and a string literal in query strings to show how they differ. The string literal gets inferred as a [Duration](https://docs.oracle.com/en/java/javase/17/docs//api/java.base/java/time/Duration.html) to add one second to the timestamp.

```python order=result,source
from deephaven import empty_table

source = empty_table(1).update(["Now = now()"])

result = source.update(
    ["NowPlusString = Now + `PT1s`", "NowPlusStringLiteral = Now + 'PT1s'"]
)
```

## Strings in tables

In the following example, a [new table](../reference/table-operations/create/newTable.md) is created with two [string columns](../reference/table-operations/create/stringCol.md).

> [!NOTE]
> [String](../reference/query-language/types/strings.md) columns can be created using single or double quotes. Double quotes are recommended, especially when using [string literals](#string-literals-in-query-strings).

```python
from deephaven import new_table
from deephaven.column import string_col

result = new_table(
    [
        string_col("X", ["A", "B", "B", "C", "B", "A", "B", "B", "C"]),
        string_col("Y", ["M", "M", "N", "N", "O", "O", "P", "P", "P"]),
    ]
)
```

### String concatenation

String columns can be concatenated in queries. The following example adds two [[string columns](../reference/table-operations/create/stringCol.md)] together with `+`.

```python order=result,source
from deephaven import new_table
from deephaven.column import string_col

source = new_table(
    [
        string_col("X", ["A", "B", "B", "C", "B", "A", "B", "B", "C"]),
        string_col("Y", ["M", "M", "N", "N", "O", "O", "P", "P", "P"]),
    ]
)

result = source.update("Add = X + Y")
```

### Escape characters

Strings in Deephaven, like Python, support the escape character `\`. The escape character invokes alternative interpretations of the characters that follow it. For example, `\n` is a new line and not the character `n`. Similarly, `\t` is a tab and not the character `t`.

The query below shows how Deephaven responds to these characters.

```python
from deephaven import new_table
from deephaven.column import string_col

result = new_table(
    [
        string_col(
            "X",
            [
                'Quote " in quotes',
                "Single quote ' in single quotes",
                "Escaped slash \\",
                "New\nline",
                "Added\ttab",
            ],
        )
    ]
)
```

> [!NOTE]
> For more information on escaping characters in strings, see the [Python documentation](https://docs.python.org/3/reference/lexical_analysis.html#strings).

### String filters

Deephaven supports using [`java.lang.String`](https://docs.oracle.com/en/java/javase/17/docs//api/java.base/java/lang/String.html) methods on [strings](../reference/query-language/types/strings.md) in queries.

The following example shows how to [filter](./use-filters.md) a [string column](../reference/table-operations/create/stringCol.md) for values that start with `"C"`.

```python order=source,result
from deephaven import new_table
from deephaven.column import string_col

source = new_table(
    [string_col("X", ["Aa", "Ba", "Bb", "Ca", "Bc", "Ab", "Bd", "Be", "Cb"])]
)

result = source.where(filters=["X.startsWith(`C`)"])
```

## Related documentation

- [User Guide: Filters](./use-filters.md)
- [Create a new table](./new-and-empty-table.md#new_table)
- [Create an empty table](./new-and-empty-table.md#empty_table)
- [User-Defined Functions](../how-to-guides/user-defined-functions.md)
- [Formulas](../how-to-guides/formulas-how-to.md)
- [Operators](../how-to-guides/formulas-how-to.md#operators)
