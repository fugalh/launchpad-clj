(ns examples.spectrum-analyzer
  (:require [clojure.pprint :refer [pprint]])
  (:import (javax.sound.sampled AudioFormat
                                AudioSystem)))

;;; 2048 samples gives a bin width
;;; and frame rate of a little over 20 Hz
(def N 2048)

(def lg
  (memoize
   (fn [x] (/ (Math/log x) (Math/log 2)))))

(def octave-bins
  (let [o (+ 1 (lg N))
        octaves (map #(Math/pow 2 %) (range o))]
    (map vector
         (drop-last octaves)
         (drop 1 octaves))))

(defn expi
  "e^(i*x) with kudos to Euler"
  [x]
  [(Math/cos x) (Math/sin x)])

(defn +i
  [[a b] [c d]]
  [(+ a c) (+ b d)])

(defn -i
  [[a b] [c d]]
  [(- a c) (- b d)])

(defn *i
  [[a b] [c d]]
  [(- (* a c)
      (* b d))
   (- (* a d)
      (* b c))])

(def twiddle
  (memoize (fn [k n]
             (expi (* -2 Math/PI (/ k n))))))
   
(defn fft
  "Return the FFT of the input block,
  whose length (N) must be a power of two.
  The result is a seq of [a b] representing
  the complex number (a + bi)"
  ([block]
   (let [n (count block)
         n2 (/ n 2)]
     (if (= 1 n)
       [[(first block) 0]]
       (let [block1 (fft (take-nth 2 block))
             block2 (fft (take-nth 2 (rest block)))]
         (concat (map (fn [k x1 x2]
                        (+i x1
                            (*i (twiddle k n)
                                x2)))
                      (range n2)
                      block1
                      block2)
                 (map (fn [k x1 x2]
                        (-i x1
                            (*i (twiddle k n)
                                x2)))
                      (range n2)
                      block1
                      block2)))))))

(defn magnitude
  [spectrum]
  (map (fn [[a b]]
         (Math/sqrt (+ (* a a)
                       (* b b))))
       spectrum))

(defn normalize
  "Normalize by 1/N"
  [spectrum]
  (let [n N]
    (map #(/ % n) spectrum)))

(defn bin-by-octave
  [spectrum]
  (let [spectrum (vec spectrum)]
    (map (fn [[a b]]
           (reduce + (subvec spectrum a b)))
         octave-bins)))

(defn dBFS
  [x]
  (* 20 (Math/log10 (Math/abs x))))

(defn ->dBFS
  [coll]
  (map dBFS coll))

(def mp3-window
  (let [pi2n (/ Math/PI N)]
    (map #(Math/sin (* pi2n
                       (+ % (/ 2))))
         (range N))))

(defn window-by
  [block window]
  (map * block window))

(defn open-audio
  []
  (let [af (AudioFormat. 44100 8 1 true false)
        tdl (AudioSystem/getTargetDataLine af)]
    (.open tdl af)
    (.start tdl)
    tdl))

(defn read-audio
  [tdl n]
  (let [buf (byte-array n)]
    (.read tdl buf 0 n)
    (vec buf)))

(defn now [] (java.util.Date.))
(defn wha
  [xs name]
  (pprint [(now) name (count xs)])
  xs)

;;; run
(defn run []
  (let [tdl (open-audio)]
    (while true
      (let [xs (read-audio tdl N)
            spectrum (-> xs
                         (window-by mp3-window)
                         (fft) ; N wide
                         (magnitude)
                         (normalize)
                         (bin-by-octave) ; 11 wide
                         (->dBFS))
            spectrum (take 8 spectrum)]
        ;;(.update pad (render-spectrum spectrum))
        (wha spectrum "loop")))))
