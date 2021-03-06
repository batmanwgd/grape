(ns grape.visualizer
  (:require
   [clojure.data.json :as json]
   [schema.core :as s]
   [clojure.string :as str]
   [clojure.set :refer :all]
   [dorothy.core :as dorothy]
   [clojure.data.codec.base64 :as b64]
   [grape.util :refer :all]
   ))

(defn reset-ctr! []
  (intern *ns* '_ctr 0))

(defn get-ctr! []
  (intern *ns* '_ctr (inc (eval '_ctr)))
  (eval '_ctr))

(defn dot->render [g]
  (dorothy/render g {:format :png}))

(defn dot->image [g]
  (String. (b64/encode (dorothy/render g {:format :png}))))

(defn dorothy->dot [g]
  (dorothy/dot g))

(defn asserts->dot [as]
  "Translate a map of assertions to Dot"
  (if (empty? as)
    ""
    (str (reduce (partial str-sep "\n") (map (fn [[k v]]
                                               (str (name k) "=" v)) as)))))

(defn node->dot [n c o]
  (let [p (second n)
        name (:id p)
        l (:label p)
        as (:asserts p)]
    (str " " name " [color=" c " shape=record penwidth=bold  " o " "
         "label=\"{" name (if (nil? l) "" (str ":" l))
         (if (empty? as)
           " "
           (str " | " (asserts->dot as)))
         " }\"]; ")))


(defn edge->dot [e c o]
  (let [p (second e)
        src (name (:src p))
        tar (name (:tar p))
        l (:label p)
        as (:asserts p)]
    (str " " src " -> " tar
  ;       " [color=" c " penwidth=bold len=2 constraint=false fontcolor=" c " " o
          " [color=" c " penwidth=bold len=2  fontcolor=" c " " o
         " label=\"" l
         (if (empty? as)
           ""
           (str "\n{" (asserts->dot as) "}"))
         "\" ]"
         )))

(declare pattern->dot)

(defn NAC->dot
  "translate a NAC to dot"
  [nac]
  (let [nacid (second nac)
        c (nth '("blue" "darkviolet" "brown" "dimgrey") nacid)
        p (nth nac 2)]
    (pattern->dot p [] c c " style=dashed ")))

(defn cond->dot
  "translate a condition to dot"
  [c]
  (str " "
       (get-ctr!)
       " cond [color=lightgrey style=filled shape=house label=\"" (second c) "\"]"))

(defn assign->dot
  "translate an assignment to dot"
  [c]
  (str " "
       (get-ctr!)
       " [color=seagreen1 style=filled shape=invhouse label=\"" (second c) "\"]"))


(defn graphelem->dot [d c1 c2 o e]
  "Translate a graph element to dorothy - either node or edge"
  (let [t (first e)
        id (:id (second e))
        c (if (nil? (some #{id} d)) c1 c2)]
    (cond
      (= 'node t) (node->dot e c o)
      (= 'edge t) (edge->dot e c o)
      (= 'NAC t) (NAC->dot e)
      (= 'cond t) (cond->dot e)
      (= 'assign t) (assign->dot e)

     :else
     (throw (Exception. "Invalid graph element"))
     )))


(defn pattern->dot
  "translate a graph pattern to dot"
  [p d c1 c2 o]
  (let [els (:els (second p))]
    (if (nil? els)
      ""
      (reduce str (map (partial graphelem->dot d c1 c2 o) els)))))

(defn rule->dot [rid]
  "translate a rule to dot"
  (reset-ctr!)
  (let [n (name rid)
        rule ((:rules (eval 'gragra)) rid)
        r (:read rule)
        d (:delete rule)
        c (:create rule)
        p (:params rule)]
    (str "digraph g {  splines=true overlap=false subgraph cluster0 {"
         "label=\"Rule: " n (str p)\";"
         (pattern->dot r d "black" "red" "")
         (pattern->dot c [] "green" "green" "")
         "}}")))


(defn document-rule [r]
  (dorothy/save! (rule->dot r) (str "doc/images/"(name r) ".png") {:format :png}))

(defn document-rules []
  (map document-rule (keys (:rules (intern *ns* 'gragra)))))

