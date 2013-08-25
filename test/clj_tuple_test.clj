(ns clj-tuple-test
  (:require
    [clojure.test :refer :all]
    [clj-tuple :refer :all]
    [criterium.core :as c])
  (:import
    [java.util.concurrent
     ConcurrentHashMap]
    [java.util
     HashMap]))

(defn equivalent? [a b]
  (is (= a b))
  (is (= b a))
  (is (every? #(= (nth a %) (nth b %)) (range (count a))))
  (is (= (apply + b) (apply + a)))
  (is (.equals ^Object a b))
  (is (.equals ^Object b a))
  (is (= (hash a) (hash b)))
  (is (= (first a) (first b)))
  (when-not (empty? a)
    (equivalent? (rest a) (rest b))))

(deftest test-equivalency
  (let [seqs (map #(range %) (range 100))]
    (doseq [s seqs]
      (equivalent? (vec s) (apply tuple s))
      (equivalent? s (apply tuple s))
      (equivalent? (apply tuple s) (apply tuple s)))))

(defmacro do-benchmark [description bench-form-fn]
  (let [bench-form-fn (eval bench-form-fn)]
    `(do
       ~@(map
           (fn [[type x]]
            `(do
               (println "\n  ** "~description ~type "\n")
               ~(bench-form-fn x)))
           (partition 2
             ["list 1"    '(list 1)
              "vector 1"  '(vector 1)
              "tuple 1"   '(tuple 1)
              "list 2"    '(list 1 2)
              "vector 2"  '(vector 1 2)
              "tuple 2"   '(tuple 1 2)
              "list 3"    '(list 1 2 3)
              "vector 3"  '(vector 1 2 3)
              "tuple 3"   '(tuple 1 2 3)
              "list 5"    '(list 1 2 3 4 5)
              "vector 5"  '(vector 1 2 3 4 5)
              "tuple 5"   '(tuple 1 2 3 4 5)
              "list 7"    '(list 1 2 3 4 5 6 7)
              "vector 7"  '(vector 1 2 3 4 5 6 7)
              "tuple 7"   '(tuple 1 2 3 4 5 6 7)])))))

(deftest ^:benchmark benchmark-construction
  (do-benchmark "create" (fn [x] `(c/quick-bench ~x))))

(deftest ^:benchmark benchmark-addition
  (do-benchmark "apply +" (fn [x] `(c/quick-bench (apply + ~x)))))

(deftest ^:benchmark benchmark-reduce
  (do-benchmark "reduce +" (fn [x] `(c/quick-bench (reduce + ~x)))))

(deftest ^:benchmark benchmark-apply
  (do-benchmark "apply"
    (fn [x]
      `(let [f# (fn [& args#] args#)
             x# ~x]
         (c/quick-bench (apply f# x#))))))

(deftest ^:benchmark benchmark-seq
  (do-benchmark "seq"
    (fn [x]
      `(c/quick-bench (seq ~x)))))

(deftest ^:benchmark benchmark-hasheq
  (do-benchmark "hasheq" (fn [x] `(c/quick-bench (hash ~x)))))

(deftest ^:benchmark benchmark-hash
  (do-benchmark "hash" (fn [x] `(c/quick-bench (.hashCode ~(with-meta x {:tag "Object"}))))))

(deftest ^:benchmark benchmark-equality
  (do-benchmark "equals"
    (fn [x]
      `(let [a# ~x
             b# ~x]
         (c/quick-bench
           (.equals ^Object a# b#))))))

(deftest ^:benchmark benchmark-get-hash-map
  (do-benchmark "get hash-map"
    (fn [x]
      `(let [x# ~x
             ^HashMap m# (doto (HashMap.) (.put x# x#))]
         (c/quick-bench (.get m# ~x))))))

(deftest ^:benchmark benchmark-get-concurrent-hash-map
  (do-benchmark "get concurrent hash-map"
    (fn [x]
      `(let [x# ~x
             ^ConcurrentHashMap m# (doto (ConcurrentHashMap.) (.put x# x#))]
         (c/quick-bench (.get m# ~x))))))

(deftest ^:benchmark benchmark-get-persistent-hash-map
  (do-benchmark "get persistent hash-map"
    (fn [x]
      `(let [x# ~x
             m# {x# x#}]
         (c/quick-bench (get m# ~x))))))
