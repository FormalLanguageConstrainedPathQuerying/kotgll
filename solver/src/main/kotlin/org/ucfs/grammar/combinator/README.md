# Grammar combinator 
Kotlin DSL for describing context-free grammars.



## Declaration

Example for A* grammar

*EBNF*
```
A = "a"
S = A* 
```
*DSL*
```kotlin
class AStar : Grammar() {
        var A = Term("a")
        var S by NT()

        init {
            setStart(S)
            S = Many(A)
        }
    }
```
### Non-terminals

`val S by NT()`

Non-terminals must be fields of the grammar class. Be sure to declare using delegation `by NT()`!!!

Start non-terminal set with method `setStart(nt)`. Can be set once for grammar.

### Terminals 

`val A = Term("a")`

`val B = Term(42)`

Terminal is a generic class. Can store terminals of any type. Terminals are compared based on their content. 

They can be declared as fields of a grammar class or directly in productions.

## Operations
Example for Dyck language

*EBNF*
```
S = S1 | S2 | S3 | ϵ
S1 = '(' S ')' S 
S2 = '[' S ']' S 
S3 = '{' S '}' S 
```
*DSL*
```kotlin
class DyckGrammar : Grammar() {
        var S by NT()
        var S1 by NT()
        var S2 by NT()
        var S3 by NT()

        init {
            setStart(S)
            S = S1 or S2 or S3 or Epsilon
            S1 = Term("(") * S * Term(")") * S
            S2 = Term("[") * S * Term("]") * S
            S3 = Term("{") * S * Term("}") * S
        }
    }
```
### Production
A → B = A = B

### Concatenation
(.): Σ∗ × Σ∗ → Σ∗

a . b = a * b

### Alternative
a | b = a or b

### Kleene Star
$a* = U_{i=0}^{\inf}a^i$

a* = Many(a)

`todo: a+ = some(a)`

### Optional 
a? -> a | Epsilon

Epsilon -- constant terminal with behavior corresponding to the $\epsilon$ terminal (empty string).

`todo: a? = opt(a)`

## RSM 
DSL allows to get the RSM corresponding to the grammar using the `getRsm` method.
The algorithm of RSM construction is based on Brzozowski derivations.
