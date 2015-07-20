{$, $$$, View}  = require 'atom-space-pen-views'

module.exports =
class HistoryView extends View

  @content: (params) ->
    @div =>
      @ol outlet: "history"

  initialize: (params) ->
    atom.prob.ui.register_handler("history-view-5e9a6977-f4de-4765-937d-9162f3603f66", @changeTrace)
    atom.prob.ui.subscribe("history-view-5e9a6977-f4de-4765-937d-9162f3603f66", '[:trace #uuid "5e9a6977-f4de-4765-937d-9162f3603f66"]')

  ppTransition: (t) ->
    console.log t.name

  changeTrace:(x,db) =>
    @ppTransition t for t in x.transitions



  getTitle: ->
    "ProB"
