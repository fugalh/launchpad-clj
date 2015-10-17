(ns launchpad
  (:require launchpad.core))

(def initial-state launchpad.core/initial-state)
(def get-launchpad launchpad.core/get-launchpad)

;;; These convenience functions work on an IState,
;;; so both State and Launchpad objects work. The same input type is
;;; returned.
;;;
;;; The implicit versions work on the default launchpad returned by
;;; get-launchpad. This is all very stateful, but interacting with a
;;; physical device is one of those things that is inherently stateful
;;; so I don't feel bad about providing an easy way (or leaving off
;;; the alarming !s).

(defn light
  ([thing what where color]
   (.light thing what where color))
  ([what where color]
   (light (get-launchpad) what where color)))

(defn unlight
  ([thing what where]
   (.unlight thing what where))
  ([what where]
   (unlight (get-launchpad) what where)))

(defn grid
  ([thing [x y] color]
   (light thing :grid [x y] color))
  ([where color]
   (grid (get-launchpad) where color)))

(defn top
  ([thing x color]
   (light thing :top x color))
  ([x color]
   (top (get-launchpad) x color)))

(defn side
  ([thing y color]
   (light thing :side y color))
  ([y color]
   (side (get-launchpad) y color)))

(defn reset [] (.reset (get-launchpad)))
