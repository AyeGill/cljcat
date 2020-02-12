# cljcat

A Clojure library designed to ... well, that part is up to you.

## Usage

FIXME

## License

Copyright © 2020 FIXME

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
