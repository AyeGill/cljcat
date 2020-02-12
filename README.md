# cljcat

A Clojure library for applied category theory. Inspired by [Catlab.jl](https://github.com/epatters/catlab).

Similarly to Catlab, cljcat is not intending to be a theorem prover.
Rather, the current idea is to use property-based testing to check invariants.
(However, this idea is not implemented yet).

## Overview

Read the example code in `core.clj` to get an idea.

The bulk of the code currently revolves around `deftheory`.
This is a macro which produces a sort of bastardized semi-algebraic theory.
A theory in this sense consists of sorts, operations, predicates, and equations (the equations currently don't matter).
Each predicate has an associated "signature", stating a list of sorts for the variables.
An operation has a more complicated signature, giving both a sort for each variable, as well as optional pre- and postconditions, expressed in terms of the predicates, the sorts, and arbitrary Clojure functions.

For instance:
```clojure
(deftheory monoid
    [element]
    []
    [e [] element]
    [mult [a element b element] element]
    [[a element b element c element] (= (mult a (mult b c) (mult (mult a b) c)))]
    [[a element] (= (mult (e) a) (mult a (e)) a)])
```
Declares the theory of monoids, which has
- A single sort, `element`
- No predicates
- A nullary operation `e` which gives an element, and a binary operation `mult`, which takes two elements and returns an element.
- Satisfies associativity and unitality.

Here is a monoid:
```clojure
(def zmonoid (reify monoid
    (-element [this x] (integer? x))
    (-e [this] 0)
    (-mult [this x y] (+ x y))))
```
Now we can do `(e zmonoid) ;0` and `(mult zmonoid 10 20) ;30` and so on.
What's up with the dashes? Deftheory generates a binding
```clojure
(defn mult [m a b]
    {:pre [(element m a) (element m b)]
     :post #(element m %)}
    (-mult m a b))
```
Which includes the pre and postconditions we put on mult.
It also generates a Clojure protocol, `monoid`, with methods `-element`, `-e` and `-mult`.

Currently the equations are completely ignored - the current goal is to make them into a spec for use with property-based testing.

## Usage

Currently unusable. See `core.clj` for examples.

## Devnotes/Todo/Roadmap

Theories/core:

- [ ] Make testing equations work at a basic level
- [ ] Generate free instances from equations using logic programming (?)
- [ ] Generate spec for structure-preserving maps (e.g functors)
- [ ] Add option to use custom equality relations for testing (e.g. extensional function equality)
- [ ] Extending theories

Quality of life:

- [ ] Macros to automatically insert instances.

Categories:

- [ ] Pullbacks
- [ ] Products
- [ ] Monoidal categories
  - [ ] Must include a way of making a category with products into a monoidal category.

## License

Copyright © 2020 Eigil Fjeldgren Rischel

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

## Stuff

Idea: `(deftheory category ...)` produces

- A record `category` containing the specification of the theory
- A `(definstance category ...)` macro which makes it easy to specify categories.

```clojure

(def category
  {:sorts [:object :morphism]
   :operations [[:id [:object] :morphism]
                [:dom [:morphism] :object]
                [:cod [:morphism] :object]
                [:compose [[:pullback :dom :cod]] :morphism]]
   :equations [[:equal [:dom [:id a]] a]
               [:equal [:cod [:id a]] a]
               [:equal [:compose f [:compose g h]] [:compose [:compose f g] h]]]})

(deftheory category
  [:object :morphism]
  [[[:object a] [:morphism [:id a]]]
   [:dom [:morphism] :object]
   [:cod [:morphism] :object]
   [:compose []]])
;Awkward: how to deal with general limit domains
;How to write equations
;Really need to use a macro/syntax for this.

[[:object a] [:morphism [:id a]]] ;deduction-style notation
[[:morphism f :morphism g [= [:cod f] [:dom g]]] [:morphism [:compose f g]]];allows for a more natural description of "partially defined maps"

;algebra of theory:
;membership predicate (and equality?) for each sort
;functions for each operation
;generates:
; tests for well-defined-ness
; tests for equations
; interpretation and well-typed-ness-checking for composed operations
```
