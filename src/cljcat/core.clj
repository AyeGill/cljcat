(ns cljcat.core)

;Should elements of an instance of a theory carry around their parent?
;For now, i've decided to not do this.
;If implementing this, do it by dispatching on metadata.

;How to generate checks.
;;How to pass in instance to methods!
;;Problem for testing:
;;Suppose we test associativity
;;Naive way to do this is to generat mors randomly, test that any which satisfy
;;assumption will satisfy postconditions.
;;Problem: Almost no random three mors will be composable!
;;Need to make gen understand conditions
;;Generate mors with specified obj
;;This seems like a hard problem!
;;Fixes:
;; Bake "parameterization" of mor sort by obs into definition
;; Make "gen solutions to equations" part of generative stuff

(defn make-condition [expr ops isymb]
  (cond
    (not (seq? expr)) expr ;just leave atoms
    (some #{(first expr)} ops)
    `(~(first expr) ~isymb ~@(map #(make-condition % ops isymb) (rest expr)));insert the instance
    :else (map #(make-condition % ops isymb) expr)))


(defn parse-arg-list
  "Parse an arglist. Returns [symbols conditions]"
  [arglist ops isymb]
  (let [stack (atom arglist)
        symbs (atom [])
        sorts (atom []);in the same order
        conds (atom [])]
    (while (not (empty? @stack))
      (cond
        (symbol? (first @stack)) (do (swap! symbs #(conj % (first @stack)))
                                     (swap! stack rest)
                                     (when-not (empty? @stack)
                                       (swap! sorts #(conj % (first @stack)))
                                       (swap! stack rest)))
        :else (do (swap! conds #(cons (make-condition (first @stack) ops isymb) %))
                  (swap! stack rest))))
    [@symbs `[~@(map (fn [sym sort] (list sort isymb sym)) @symbs @sorts)
               ~@@conds]]))


(defn parse-pred [pred all-ops isymb]
  (cond
    (some #{pred} all-ops) `#(~pred ~isymb %) ;if an operation, add i
    (symbol? pred) pred ;otherwise just treat it as a predicate - can be left
    (seq? pred) `(fn [~(first pred)]
                   (and ~@(map #(make-condition % all-ops isymb) (rest pred))))))

(defn parse-postconds [preds all-ops isymb]
  (vec (map #(parse-pred % all-ops isymb) preds)))

;A theory is a clojure protocol. Then e.g. finset is an object which implements this protocol.
;TODO: Given an operation foo, (foo instance) should produce a function wrapped in specs
;(which, of course, depend on the instance)
(defn tweak-sym 
  "foo -> -foo"
  [sym]
  (symbol (str "-" sym)))


(defmacro deftheory [name sorts predicates operations & equations]
  (let [all-ops (concat sorts (map first operations))
        i (gensym "i")]
    `(do (defprotocol ~name
           ~@(for [sort sorts]  `(~(tweak-sym sort) [i# a#]
                                                    ~(str "Check if a value has sort " sort)))
        ;for each sort, do (sort instance a) to check if a has sort in instance
           ~@(for [[op arglist & preds] operations]
               `(~(tweak-sym op) [i# ~@(first (parse-arg-list arglist [] 'i))]))
           ~@(for [[pred arglist] predicates]
               `(~(tweak-sym pred) [i# ~@(first (parse-arg-list arglist [] 'i))])))
         ~@(for [sort sorts]
             `(defn ~sort
                ([i# a#] (~(tweak-sym sort) i# a#))
                ([i#]
                 {:pre (satisfies? ~name i#)}
                 (partial ~sort i#))
                ([i# a# & as#] (every? (~sort i#) (cons a# as#)))))
         ~@(for [[op arglist & preds] operations]
             (let [[symbs preconds] (parse-arg-list arglist all-ops i)
                   postconds (parse-postconds preds all-ops i)]
               `(defn ~op
                  ([~i] (partial ~op ~i))
                  ([~i ~@symbs]
                   {:pre [~@preconds]
                    :post ~postconds}
                   (~(tweak-sym op) ~i ~@symbs)))))
         ~@(for [[pred arglist] predicates]
             (let [[symbs preconds] (parse-arg-list arglist all-ops)]
               `(defn ~pred
                  ([~i] (partial ~pred ~i))
                  ([~i ~@symbs]
                   {:pre [~@preconds]
                    :post #(boolean? %)}
                   (~(tweak-sym pred) ~i ~@symbs)))))
        ;;TODO add the equations as tests
         )))


(deftheory category
  [mor obj] ;sorts
  [];no predicates
  [[dom [f mor] obj] ;operations
   [cod [f mor] obj] ;predicate(s) for result
   [id [x obj] (f (mor f) (= (cod f) (dom f) x))]
   ;vector first symbol is paramter, takes value of result.
   ;Rest are predicates about result. Can use parameters here
   ;Note at this stage, can't have cycles of invariants!
   [compose [f mor g mor (= (cod f) (dom g))]
    ;vectors in arg list are invariants
    mor (h (= (cod h) (cod g)) (= (dom h) (dom g)))]]
  ;the rest are axioms
  [[f mor] ;assumptions
   [= [compose f [id [cod f]]] f] ;conclusions (unitality)
   [= [compose [id [dom f]] f] f]]
  [[f mor g mor h mor
    [= [cod f] [dom g]]
    [= [cod g] [dom h]]] [= [compose f [compose g h]]
                          [compose [compose f g] h]]])

(defn compose-seq
  ([c f] {:pre (mor c f)} f) ;should have (category c) cond
  ([c f & more] (compose c f (apply compose-seq c more))))
;(def finset (reify category
;              (object [this a] (set? a))
;              (morphism [this a] ...)))

;Reify for theories

(def finset
  (reify category
    (-obj [i a] (set? a))
    (-mor [i a] (let [{cod :cod dom :dom f :map} a]
                  (and (set? cod)
                       (set? dom)
                       (every? cod (map f dom)))))
    (-dom [i a] (:dom a))
    (-cod [i a] (:cod a))
    (-id [i a] {:cod a :dom a :map identity})
    (-compose [i f g] {:dom (:dom f)
                       :cod (:cod g)
                       :map (comp (:map g) (:map f))})))
;(e.g (->free-cat [:a :b :c] {:f [:a :b] :g [:a :c]}]))
;code morphisms as sequences, including a specified start and end object
;E.g. [:a :f :c] or [:a :g :c]
;Identities are then [:a :a]
(defrecord free-cat
           [object-syms morphism-sigs]
  category
  (-obj [this a] (boolean (some #{a} object-syms)))
  (-mor [this f]
    (and (vector? f)
         (obj this (first f))
         (obj this (last f))
         (or
          (and (= (first f) (last f))
               (= (count f) 2))
          (and 
           (every? (fn [[[x y] [z w]]] (= y z)) (partition 2 1 (map #(get morphism-sigs %) (rest (drop-last f)))))
           (= (first f) (first (get morphism-sigs (second f))))
           (= (last f) (second (get morphism-sigs (last (drop-last f)))))))))
  (-dom [this f] (first f))
  (-cod [this f] (last f))
  (-id [this a] [a a])
  (-compose [this f g]
            (concat (drop-last f) (rest g)))) 

(defn finfn [dom cod f] {:dom dom :cod cod :map f})

(defn setprod [x y]
  (let [p (set (for [a x b y] [a b]))]
    [p {:dom p :cod x :map first} {:dom p :cod y :map second}]))
(defn setpair [f g]
  {:dom (:dom f) :cod (first (setprod (:cod f) (:cod g)))
   :map (fn [[x y]] [((:map f) x) ((:map f) y)])})
  
;eg (freefunctor {:a #{1 2} :b #{3 4}} {:f [:a :b]} {:f {:cod ...}})
(defn freefunctor [freecat target ob-map mor-map]
  (fn [x] 
    (cond
      (obj freecat x) (x ob-map);objects map to the assigned thing
      (mor freecat x) (if (= (count x) 2)
                        (id target (first x))
                        (compose-seq target (rest (drop-last x)))))))

;This should somehow generate code that, given a category and a tuple,
;checks that it really does present a product
;(... product [p x y f g pair]
;    [object p x y]
;     [morphism f g]
;     [= [dom f] [dom g] p]
;     [= [:cod f] x]
;     [= [:cod f] y]
;     [:function pair [h i] [[:morphism h i]
;                            [= [:cod h] x]
;                            [= [:cod i y]]
;                            [= [:dom i] [:dom h]]]
;      [[:morphism [pair h i]]
;       [= [:dom [pair h i]] [:dom h]]
;       [= [:cod [pair h i]] p]
;       [= [:compose [pair h i] f] h]
;       [= [:compose [pair h i] g] i]
;       [= [pair [:compose j f] [:compose j g]] j]]]) ;infer conditions on j from context.
  
;Then we should be able to write code like:
;(defn setprod [x y]
;  (let [p (set (for [a x b y] [a b]))]
;    [p {:dom p :cod x :map first} {:dom p :cod y :map second}]))
;(defn setpair [f g]
;  {:cod (:cod f) :dom (first (setprod (:cod f) (:cod g)))
;   :map (fn [x] [((:map f) x) ((:map f) y)])})
  
;(assert (let [[p f g] (setprod x y)] (product p x y f g setpair)))
;This automatically generates tests, and makes the above available to logic programming
;Is it easier to do this in Haskell, using typeclasses and QuickCheck?