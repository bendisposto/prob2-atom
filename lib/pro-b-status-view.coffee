{View} = require 'atom-space-pen-views'


module.exports =
class StatusView extends View
  @content: ->
    @div class: "pro-b inline-block", =>
      @span id: 'probstatus', click: 'log' , class: "offline", "ProB"

  destroy: ->
    @detach()

  setConnectionStatus: (status) ->
    document.getElementById("probstatus").setAttribute 'class', if status then 'online' else 'offline'

  log: (event, _) ->
    {BufferedProcess} = require 'atom'
    command = 'ps'
    args  = ['-ef']
    stdout = (output) -> console.log(output)
    exit = (code) -> console.log("ps -ef exited with #{code}")
    process = new BufferedProcess({command, args, stdout, exit})
