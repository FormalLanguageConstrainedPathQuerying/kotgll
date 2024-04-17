
# UCFS

> Note: project under heavy development!

## About
**UCFS** is an **U**niversal **C**ontext-**F**ree **S**olver: a tool to solve problems related to context-free and regular language intersection. Examples of such problems:
- Parsing
- Context-free path querying (CFPQ)
- Context-free language reachability (CFL-R)

<!-- Online -- offline modes.    
    
All-pairs, multiple-source. All-paths, reachability.     
    
Incrementality. Both the graph and RSM    
    
Error recovery.    
    
 GLL-based    
 RSM    
-->    

## Project structure 
```
├── solver                  -- base ucfs logic
├── benchmarks              -- comparison with antlr4
├── generator               -- parser and ast node classes generator
└── test-shared             -- test cases 
    └── src
        └── test
            └── resources   -- grammars' description and inputs
```

## Core Algorithm
UCFS is based on Generalized LL (GLL) parsing algorithm modified to handle language specification in form of Recursive State Machines (RSM-s) and input in form of arbitratry directed edge-labelled graph. Basic ideas described [here](https://arxiv.org/pdf/2312.11925.pdf). 

## Grammar Combinator

Kotlin DSL for describing context-free grammars.



### Declaration

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

Non-terminals must be fields of the grammar class. Make sure to declare using delegation `by NT()`!

Start non-terminal set with method `setStart(nt)`. Can be set only once for grammar.

### Terminals

`val A = Term("a")`

`val B = Term(42)`

Terminal is a generic class. Can store terminals of any type. Terminals are compared based on their content.

They can be declared as fields of a grammar class or directly in productions.

### Operations
Example for Dyck language

*EBNF*
``` 
S = S1 | S2 | S3 | ε    
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
$A \Longrightarrow B \hspace{4pt} \overset{def}{=} \hspace{4pt} A = B$

### Concatenation
$( \hspace{4pt} \cdot \hspace{4pt} ) : \sum_∗ \times \sum_∗ → \sum_∗$

$a \cdot b \hspace{4pt} \overset{def}{=} \hspace{4pt} a * b$

### Alternative
$( \hspace{4pt} | \hspace{4pt} ) : \sum_∗ \times \sum_∗ → \sum_∗$

$a \hspace{4pt} | \hspace{4pt} b \hspace{4pt} \overset{def}{=} \hspace{4pt} a \hspace{4pt} or \hspace{4pt} b$

### Kleene Star
$( \hspace{4pt} ^* \hspace{4pt} ) : \sum→ \sum_∗$

$a^* \hspace{4pt} \overset{def}{=} \hspace{4pt} \displaystyle\bigcup_{i = 0}^{\infty}a^i$

$a^* \hspace{4pt} \overset{def}{=} \hspace{4pt} Many(a)$

$a^+ \hspace{4pt} \overset{def}{=} \hspace{4pt} Some(a)$

### Optional
$a? \hspace{4pt} \overset{def}{=} \hspace{4pt} a \hspace{4pt} or \hspace{4pt} Epsilon$

Epsilon -- constant terminal with behavior corresponding to the $\varepsilon$ -- terminal (empty string).

$a? \hspace{4pt} \overset{def}{=} \hspace{4pt} Opt(a)$

### RSM
DSL provides access to the RSM corresponding to the grammar using the `getRsm` method.    
The algorithm for RSM construction is based on Brzozowski derivations.
