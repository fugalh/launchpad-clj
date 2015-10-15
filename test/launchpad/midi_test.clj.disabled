(ns launchpad.midi-test
  (:require [clojure.test :refer :all]
            [launchpad.core :as c]
            [launchpad.midi :as m]
            midi)
  (:import (launchpad.core Message)))

(defn midi-eq? [a b]
  (let [fs [#(.getStatus %)
            #(.getCommand %)
            #(.getData1 %)
            #(.getData2 %)]]
    (apply = (map (fn [f] (fn [m] (map f m)) fs) [a b]))))

(deftest test-msg->midi
  (testing "grid"
    (is (midi-eq? (m/msg->midi (Message. :grid [2 3] [1 3]))
                  (midi/note-on 0x32 0x3d))))
  (testing "top"
    (is (midi-eq? (m/msg->midi (Message. :top 4 [2 0]))
                  (midi/control-change 0x6c 2))))
  (testing "right"
    (is (midi-eq? (m/msg->midi (Message. :right 7 [0 3]))
                  (midi/note-on 0x78 0x3c)))))

(deftest test-midi->msg
  (testing "grid"
    (is (= (m/midi->msg (midi/note-on 0x32 0x3d))
           (Message. :grid [2 3] [1 3]))))
  (testing "top"
    (is (= (m/midi->msg
            (midi/control-change 0x6c 2))
           (Message. :top 4 [2 0]))))
  (testing "right"
    (is (= (m/midi->msg (midi/note-on 0x78 0x3c))
           (Message. :right 7 [0 3])))))


