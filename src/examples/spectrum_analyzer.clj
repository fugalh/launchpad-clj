(ns examples.spectrum_analyzer
  (:require [launchpad.core :as lp])
  (:import (javax.sound.sampled AudioFormat
                                AudioSystem))
  (:gen-class))

;;; The FFT stuff. (Could I use a library? Of course, but I hadn't implemented
;;; an FFT in awhile...)

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

(defn normalize-to
  "Normalize magnitude to the given maximum."
  [spectrum max-mag]
  (if (= 0.0 max-mag)
    spectrum
    (map #(/ % max-mag) spectrum)))
  
(defn bin-by-octave
  [spectrum]
  (let [spectrum (vec spectrum)]
    (map (fn [[a b]]
           (/ (reduce + (subvec spectrum a b)) (- b a)))
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

;;; The Launchpad stuff
(defn render-column
  [c dbfs]
  (let [y (- (int (max -8 dbfs)))]
    (if (<= 0 y 7)
      (reduce merge {}
              (for [yy (range y 8)] {[c yy] [0 3]}))))) ; TODO pretty colors
        
(defn render-spectrum
  "Render the spectrum to a Launchpad"
  [spectrum]
  (let [s lp/initial-state
        db-range 96 ; 16-bit resolution (ignore dithering)
        ;; but really music is usually only half that dynamic range
        db-range (/ db-range 2)
        spectrum (map #(* 8 (/ % db-range)) spectrum)] ; normalize
    (assoc s :grid
           (reduce merge {}
                   (map render-column
                        (range 8)
                        spectrum)))))

;;; run
(defn run []
  (let [tdl (open-audio)
        pad (lp/get-launchpad)]
    (.reset pad)
    (loop [max-mag 0]
      (let [xs (read-audio tdl N)
            spectrum (-> xs
                         (window-by mp3-window)
                         (fft) ; N wide
                         (magnitude)
                         (bin-by-octave)) ; 11 wide
            max-mag (apply max max-mag spectrum)
            spectrum (->dBFS (normalize-to spectrum max-mag))]
        (.render pad (render-spectrum spectrum))
        (recur max-mag)))))

(defn -main
  []
  (run))

;;; TODO
;;; pretty colors and peaks
;;; dynamic range zoom in/out (/8 or *8)
