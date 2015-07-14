{View} = require 'atom-space-pen-views'


module.exports =
class StatusView extends View
  @content: ->
    @div class: "pro-b inline-block", =>
      @span click: 'log' , class: "status-offline", "ProB"
      @span outlet: 'statusText'

  initialize: () ->
    console.log 'Hiya'



  destroy: ->
    @detach()

  update: (status) =>
    @statusText.text(status)

  log: (event, _) ->
    console.log "D'oh!"
    console.log event
    {BufferedProcess} = require 'atom'
    command = 'ps'
    args  = ['-ef']
    stdout = (output) -> console.log(output)
    exit = (code) -> console.log("ps -ef exited with #{code}")
    process = new BufferedProcess({command, args, stdout, exit})
