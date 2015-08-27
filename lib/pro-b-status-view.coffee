{View} = require 'atom-space-pen-views'

module.exports =
class StatusView extends View
  @content: ->
    @div class: "pro-b inline-block", =>
      @span outlet: "status", id: 'probstatus', click: 'log' , class: "offline", "ProB"

  destroy: ->
    @detach()

  initialize: (params) ->
    atom.prob.ui.register_handler("status-view", @setStatus)
    atom.prob.ui.subscribe("status-view", '[:connected]')
    #atom.prob.ui.register_handler("trace-view", @showList)
    #atom.prob.ui.subscribe("trace-view", '[:trace-list]')

  setStatus: (x) =>
    if x
      @status.addClass("online")
    else
      @status.removeClass("online")

  showList: (x) =>
    console.log "a", x

  log: (event, _) ->
    {BufferedProcess} = require 'atom'
    command = 'ps'
    args  = ['-ef']
    stdout = (output) -> console.log(output)
    exit = (code) -> console.log("ps -ef exited with #{code}")
    process = new BufferedProcess({command, args, stdout, exit})
