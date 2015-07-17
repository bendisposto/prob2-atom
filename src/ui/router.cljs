(ns ui.router
  (:require-macros [reagent.ratom :as ra :refer [reaction]])
  (:require [cljs.nodejs :as node]
            [taoensso.sente  :as sente]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [taoensso.encore :as enc  :refer (logf log logp)]))

(def prob (atom nil))

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


(rf/register-sub
  :connected
  (fn [db _]
    (reaction (:connected @db))))

(rf/register-handler
  :connection-status
  rf/debug
  (fn [db [_ status]]
     (.setConnectionStatus (.-statusView @prob) status)
     (assoc db :connected status)))

(rf/register-handler
    :message
    rf/debug
    (fn [db [_ msg]] (logp :received msg) db))

(rf/register-handler
  :init
  rf/debug
  (fn [db _] (assoc db :sente (init-websocket))))

  (defn ^:extern subs-handler
    "Forwards changes to a subscription to a handler. First argument
    is the handler id that should be triggered. The second argument
    is a subscription pattern. All following arguments are added to
    the handler dispatch call. All arguments are preprocessed by the
    Clojurescript reader. The handler is called with the changed
    subscription. The arguments are also passed into this funtion.
    The registration of the handler must be done separately.

    For instance: We have a handler registered that uses the key :foo
    and we want to trigger :foo each time the trace with id
    09b2fdfa-f49b-4c5f-be64-ea5e63f0d628 changes.

    de.prob2.subs.subs_handler(':foo', '[:trace #uuid \"09b2fdfa-f49b-4c5f-be64-ea5e63f0d628\"])

    A matching handler could be registered with
    de.prob2.subs.register_handler(
    ':foo',
    function(x,db) { console.log('changed',x,
    'fulltree',db,
    'args',arguments);})

    Important: A handler should not take longer than 16ms to run. Long
    running handlers are supposed to split up the work into chunks and
    call themselfs after 16 ms. Information for splitting up the work
    can be encoded in the optional arguments.
    "
    [handler hook & args]
    (let [real-handler (cljs.reader/read-string handler)
          real-hook (cljs.reader/read-string hook)
          x (rf/subscribe real-hook)]
      (ra/run! (rf/dispatch
                (into [real-handler
                       (clj->js @x)]
                      args)))))


  (defn ^:extern register-handler
    "Registers a handler function. The first argument is the handler key,
    the second argument is a function of at least two arguments.
    The first argument is the changed part of the state. The second argument
    is the full state. Additional arguments are those that were passed from
    the subscription.

    Important: A handler should not take longer than 16ms to run. Long
    running handlers are supposed to split up the work into chunks and
    call themselfs after 16 ms. Information for splitting up the work
    can be encoded in the optional arguments.
    "
    [handler f]
    (let [real-handler (cljs.reader/read-string handler)]
      (rf/register-handler
       real-handler
       (fn [db [_ x & args]]
         (let [jsdb (clj->js db)
               jsx (clj->js x)
               res (apply f jsx jsdb args)]
           db)))))

(defn ^:export start []
  (logp :atom js/atom)
  (logp :prob (.-prob js/atom))
  (reset! prob (.-prob js/atom))
  (set! (.-ui @prob) ui.router)
  (rf/dispatch [:init]))

(set! cljs.core/*main-cli-fn* start)
