(ns example-cljs.core
  (:require [example-cljs.renderer :as renderer]
            [example-cljs.state :refer [state]]))

; Shadow cljs hook
(defn ^:def/after-load
  after-load
  []
  (renderer/start!)
  (reset! state {:messages []})
  )

; Start everything, called in the index html file
(defn ^:export
  main []
  (renderer/start!)
  )
