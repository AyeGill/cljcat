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


