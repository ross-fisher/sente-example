(ns example.server
  (:require
      [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
      [ring.middleware.resource :refer [wrap-resource]]
      [hiccup.core :as hiccup]
      [org.httpkit.server :as http-kit]
      [bidi.bidi :as bidi]
      [bidi.ring :refer [make-handler]]
      [taoensso.sente :as sente]
      [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
      [taoensso.sente.packers.transit :as sente-transit]
      [clojure.core.async :as async :refer [<! <!! >! >!! put! chan go go-loop]]
      [clojure.pprint :refer [pprint]])
  (:gen-class))


; Setting up sente
#_(reset! sente/debug-mode?_ true)

(let [packer (sente-transit/get-transit-packer) ; using transit to pack data
      chsk-server (sente/make-channel-socket-server!
                    (get-sch-adapter) {:packer packer
                                       ; for more information on users
                                       ; https://github.com/ptaoussanis/sente/issues/118
                                       :user-id-fn (fn [ring-req] 1)
                                       })
      {:keys [ch-recv send-fn connected-uids ajax-post-fn
              ajax-get-or-ws-handshake-fn]}
      chsk-server]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv) ; Receive channel
  (def chsk-send! send-fn) ; send api fn
  (def connected-uids connected-uids)) ; Watchable read only atom of user ids


; adding a watch to the connected uids clojure atom to be notified on change
(add-watch connected-uids :connected-uids
  (fn [_ _ old new]
    (when (not= old new)
      (println (str "New user connected")))))


; hiccup for index page
(defn landing-page-handler [req]
  (hiccup/html
    [:head
     [:meta {:charset "UTF-8"}]
      [:title "Example"]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:link {:rel "stylesheet" :href "https://unpkg.com/tachyons/css/tachyons.min.css"}]]
    [:body.w-100.h-100
      ; this csrf token is put into this div and the information is grabbed by the client
      (let [csrf-token (:anti-forgery-token req)]
        [:div#csrf-token {:csrf-token csrf-token}])
      [:h1 "Header"]
      [:div#app-container.w-100.h-100]]
    ; loading the clojurescript code
    [:script {:src "js/main.js"}]
    [:script "example_cljs.core.main()"]))


; bidi route definition
(def routes
  ["" {"/" {:get (fn [req]
                   (let [res (landing-page-handler req)]
                     {:status 200
                      :headers {"Content-Type" "text/html"}
                      :body res}))}
       "/chsk" {:get (fn [req] (ring-ajax-get-or-ws-handshake req))
                :post (fn [req] (ring-ajax-post req))}}])


; Defining the ring handler and middleware
(def ring-handler
  (-> (make-handler routes)
      (wrap-defaults site-defaults)))



(defmulti -event-handler :id)

; wrapper for event handler, could add error handling here
(defn event-handler [{:as ev :keys [id ?data event]}]
  (-event-handler ev))

(defmethod -event-handler
  :default
  [{:as ev :keys [id ?data event ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid (:uid session)]
    (printf "Unhandled event: %s\n" event)
    (when ?reply-fn
      (?reply-fn {:echo event}))))


; An example sente event which the front end can invoke to
; send this message back to it
(defn example-send-event
  []
  (doseq [uid (:any @connected-uids)]
    ; event ids must be 'namespaced' so there must be a / in the keyword
    (chsk-send! uid [:abc/def "Example Message"])))


; How the frontend will invoke the example event
(defmethod -event-handler
  :example/test-event-send
  [ev]
  (example-send-event))


; Will broadcast a message every 10 second
(defn broadcast! []
  (go-loop []
    (<! (async/timeout 10000))
    (doseq [uid (:any @connected-uids)]
      (chsk-send! uid [:my/message "Broadcast message"]))
    (recur)))


; Start an event router to handle the sente events
(defonce router (atom nil))

(defn stop-router! [] (when-let [stop-fn @router] (stop-fn)))

(defn start-router! []
  (stop-router!)
  (reset! router
    (sente/start-server-chsk-router! ch-chsk event-handler)))


;; Using http kit to start a the server
(defonce web-server (atom nil))

(defn stop-web-server! []
  (when-let [stop-fn @web-server] (stop-fn)))

(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [port (or port 0)
        [port stop-fn]
        (let [stop-fn (http-kit/run-server ring-handler {:port port})]
          [(:local-port (meta stop-fn))
           (fn [] (stop-fn :timeout 1000))])
        uri (format "http://localhost:%s/" port)]
    (printf "Server running at %s" uri)
    (try ; Will open the landing page in the browser
         (.browse (java.awt.Desktop/getDesktop)
                  (java.net.URI. uri))
         (catch java.awt.HeadlessException _))
    (reset! web-server stop-fn)))


; Full thing
(defn stop! []
  (stop-router!)
  (stop-web-server!))

(defn start! []
  (start-router!)
  (start-web-server! 8888)
  (broadcast!))

(defn -main []
  (start!)
  )


; Configure is called once in the shadow-cljs process.
; Starts the server.
(defn shadow-configure
  {:shadow.build/stage :configure}
  [build-state & args]
  (-main)
  build-state)
