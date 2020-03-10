(ns example-cljs.state
  (:require [reagent.core :as r])
  )

; use r/atom to cause dependent elements to reload
(def state (r/atom {:messages []}))

(comment
  state
  @state
         )
