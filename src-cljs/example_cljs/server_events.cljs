(ns example-cljs.server-events
  (:require [taoensso.sente :as sente]
            [taoensso.sente.packers.transit :as sente-transit]
            [cljs.core.async :refer [<! >! take! put! chan]]
            [goog.string :refer [format]]
            [example-cljs.state :refer [state]]
            ))


(defn printf [-str & args]
  (println (apply format -str args)))


(def ?csrf-token
  (when-let [el (.getElementById js/document "csrf-token")]
    (.getAttribute el "csrf-token")))


(if ?csrf-token
  (.log js/console "CSRF Detected.")
  (.log js/console "CSRF Not Detected!"))


(let [rand-chsk-type :auto
      packer (sente-transit/get-transit-packer)
      {:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client!
        "/chsk" ; must match server ring routing URL
        ?csrf-token
        {:type rand-chsk-type
         :packer packer})]

  (def chsk chsk)
  (def ch-chsk ch-recv) ; channel socket's receive channel
  (def chsk-send! send-fn) ; channel socket's send api fn
  (def chsk-state state) ; watchable, read-only atom
)


(defmulti -event-handler
  "Multimethod to handle Sente event-msgs"
  :id)


(defn event-handler
  "Wraps `-event-msg-handler` with logging, error catching,, etc."
  ; id is the name of the event (such as :chsk/recv) and data is something like
  ; :push/is-fast "hello 97!"
  [{:as ev-msg :keys [id ?data event]}]
  (-event-handler ev-msg))


(defmethod -event-handler
  :default
  [{:as ev-msg :keys [id ?data event]}]
  (printf "Unhandled event: %s, %s, %s" id event ?data)
  (printf "Unhandled event: %s" event))


(defmethod -event-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (let [[old-state-map new-state-map] ?data]
     (if (:first-open? new-state-map)
       (printf "Channel socket successfully established: %s" new-state-map)
       (printf "Channel socket state change: %s" new-state-map))))


(defmethod -event-handler :chsk/recv
  [{:as ev-msg :keys [id ?data event]}]
  (swap! state update :messages conj ?data)
  (printf "Push event %s from server: %s" id (first ?data)))


;  (printf "Push event %s from server: %s" id ?data))


(defmethod -event-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (printf "Handshake: %s" ?data)))


(defn test-event-send []
  (chsk-send! [:example/test-event-send "sneed"]))



;; Sente Event Router
(defonce router (atom nil))
(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router
          (sente/start-client-chsk-router!
            ch-chsk event-handler)))

(defn start! [] (start-router!))
(defonce start-once (start!))

(comment
  (printf "okay %s" 2)
  )
