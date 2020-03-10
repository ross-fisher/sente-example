; This file is a fix for cljs.user not found error upon hot reloading
; that can occur sometimes. Might need it.
(ns cljs.user
  (:require
    [cljs.repl :refer (Error->map apropos dir doc error->str ex-str ex-triage
                       find-doc print-doc pst source)]
    [clojure.pprint :refer (pprint)]
    [clojure.string :as string]))
