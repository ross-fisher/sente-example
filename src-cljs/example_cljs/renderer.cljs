(ns example-cljs.renderer
  (:require [reagent.core :as r :refer [atom]]
            [example-cljs.server-events :as server-events]
            [example-cljs.state :refer [state]]))

(def button1 :a.f3.no-underline.hover-bg-black.hover-white.inline-flex.items-center.pa3.ba.border-box.mr4)

(defn button [text f]
  [button1 {:value text :on-click f :class "btn mt2" :style {:width 300}} text])

(defn content []
  (fn []
    [:div.pa2.flex.flex-column
     [button "Console Log" #(js/console.log "Hello")]
     [button "Test Event Send" server-events/test-event-send]
     [:ul
      (for [[i m] (map-indexed vector (:messages @state))]
          ^{:key i} [:li (second m)])
      ]
     ]
    )
  )

(defn start! []
  (r/render [content] (js/document.getElementById "app-container")))


(comment
  (for [m (:messages @state)]
    (str m))
  )
