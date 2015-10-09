(ns launchpad.core-test
  (:require [clojure.test :refer :all]
            [launchpad.core :as lp]))

(defrecord MockRx [messages]
  javax.sound.midi.Receiver
  (close [this]
    (swap! messages #(conj % "close"))
    nil)
  (send [this msg ts] (swap! messages #(conj % msg))
    nil))
(defn new-mock-rx [] (MockRx. (atom [])))

(use-fixtures :each
     (fn [f]
       (do 
         (def pad (lp/new-launchpad lp/initial-state (new-mock-rx)))
         (def state (.state pad)))
       (f)))

(defn apply-grid [f state]
  (dotimes [x 8]
    (dotimes [y 8]
      (f (get-in state [:grid x y])))))

(defn apply-top [f state]
  (dotimes [x 8]
    (f (get-in state [:top x]))))

(defn apply-right [f state]
  (dotimes [x 8]
    (f (get-in state [:right x]))))

(defn apply-all [f state]
  (apply-grid f state)
  (apply-top f state)
  (apply-right f state))

(deftest initialized
  (apply-all #(is (= [0 0] %)) @state))

(defn rand-button []
  {:x (rand-int 8)
   :y (rand-int 8)
   :red (rand-int 4)
   :green (rand-int 4)})

(deftest grid
  (let [b (rand-button)
        x (:x b)
        y (:y b)
        red (:red b)
        green (:green b)]
    (.grid pad [x y] [red green])
    (is (= [red green]
           (get-in @state [:grid x y])))))

(deftest top
  (let [b (rand-button)
        x (:x b)
        red (:red b)
        green (:green b)]
    (.top pad x [red green])
    (is (= [red green]
           (get-in @state [:top x])))))

(deftest right
  (let [b (rand-button)
        y (:y b)
        red (:red b)
        green (:green b)]
    (.right pad y [red green])
    (is (= [red green]
           (get-in @state [:right y])))))

(deftest reset-is-one-message
  (.reset pad)
  (is (= 1 (count @(.messages (.device pad))))))

(deftest grid-midi
  (.grid pad [3 4] [2 3])
  (let [m (last @(.messages (.device pad)))]
    (is (= 0 (.getChannel m)))
    (is (= 0x90 (.getCommand m)))
    (is (= 0x43 (.getData1 m)))
    (is (= 0x3e (.getData2 m)))))

(deftest top-midi
  (.top pad 2 [0 1])
  (let [m (last @(.messages (.device pad)))]
    (is (= 0 (.getChannel m)))
    (is (= 0xb0 (.getCommand m)))
    (is (= 0x6a (.getData1 m)))
    (is (= 0x1c (.getData2 m)))))

(deftest right-midi
  (.right pad 7 [1 0])
  (let [m (last @(.messages (.device pad)))]
    (is (= 0 (.getChannel m)))
    (is (= 0x90 (.getCommand m)))
    (is (= 0x78 (.getData1 m)))
    (is (= 0x0d (.getData2 m)))))
