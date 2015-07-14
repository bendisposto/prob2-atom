(ns ui.router
  (:require [cljs.nodejs :as node]
            [taoensso.sente  :as sente]
            [re-frame.core :as rf]
            [taoensso.encore :as enc  :refer (logf log logp)]))

(defn init-websocket []
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! (str "127.0.0.1" ":" 3000 "/updates") {:type :auto})]
    (add-watch state :connection-observer (fn [_ _ _ new] (rf/dispatch [:connection-status (:open?  new)])))
    {:chsk chsk
     :ch-chsk ch-recv
     :send! send-fn
     :chsk-state state
     :stop! (sente/start-chsk-router!
             ch-recv
             (fn [e]
               (rf/dispatch [:message (:?data e)])
               #_(when (= (:id e) :chsk/recv)
                 (dispatch (vec (:?data e))))))}))


(rf/register-handler
  :connection-status
  rf/debug
  (fn [db _] (logp :connected) db))

(rf/register-handler
    :message
    rf/debug
    (fn [db [_ msg]] (logp :received msg) db))

(rf/register-handler
  :init
  rf/debug
  (fn [db _] (assoc db :sente (init-websocket))))

(defn start []
  (rf/dispatch [:init]))

(set! cljs.core/*main-cli-fn* start)
