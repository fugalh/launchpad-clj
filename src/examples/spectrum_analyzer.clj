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

(defn fft
  "Return the FFT of the input block,
  whose length (N) must be a power of two.
  The result is a seq of [a b] representing
  the complex number (a + bi)"
  [block]) ; TODO

(defn mdct
  "The modified discrete cosine transform of the two
  input blocks, which must both be the same size, and
  generally would be 'lapped', i.e. last half of the block for
  this operation becomes the first half of the next.

  This is slow (O(N^2)) but easy to code and does basically
  the same thing as FFT (maybe a little nicer due to the 
  lapping consideration)"
  [xs]
  (let [N (/ (count xs) 2)
        pin (/ Math/PI N)]
    (map (fn [k]
           (reduce + (map (fn [n]
                            (* (nth xs k)
                               (Math/cos
                                (* pin
                                   (+ n
                                      (/ (+ 1 N) 2))
                                   (+ k (/ 2))))))
                          (range (* 2 N)))))
         (range N))))

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

;; a companion to the mdct, length is 2N
(def mp3-window
  (let [NN (* 2 N)
        pi2n (/ Math/PI NN)]
    (map #(Math/sin (* pi2n
                       (+ % (/ 2))))
         (range NN))))

(defn window-by
  [block window]
  (map * block window))

(defn open-audio
  []
  (let [af (AudioFormat. 44100 16 1 true false)
        tdl (AudioSystem/getTargetDataLine af)]
    (.open tdl)
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
  (pprint (str (now) ": " name " " (take 3 xs)))
  xs)

;;; run
(defn run []
  (let [tdl (open-audio)]
    (loop [block1 (read-audio tdl N)]
      (let [block2 (read-audio tdl N)
            xs (concat block1 block2)
            spectrum (-> xs
                         (window-by mp3-window)
                         (mdct) ; N wide
                         (normalize)
                         (bin-by-octave) ; 11 wide
                         (->dBFS))
            spectrum (take 8 spectrum)]
        ;;(.update pad (render-spectrum spectrum))
        (wha spectrum "oof")
        (recur block2)))))
