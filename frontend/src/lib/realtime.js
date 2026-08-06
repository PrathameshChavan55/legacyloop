import { Client } from '@stomp/stompjs'
import { useEffect, useRef } from 'react'
import { tokens } from './api'

/**
 * Subscribes to one STOMP topic for as long as the component is mounted.
 *
 * The original opened two sockets (one for chat, one for notifications) through SockJS with a
 * reconnect manager and a presence heartbeat. The browser has WebSocket built in, the backend
 * exposes one endpoint, and stompjs reconnects on its own — so this is the whole client.
 */
export function useRealtime(topic, onMessage) {
  const handler = useRef(onMessage)
  handler.current = onMessage

  useEffect(() => {
    const accessToken = tokens.read()?.accessToken
    if (!topic || !accessToken) return undefined

    const url = `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`
    const client = new Client({
      brokerURL: url,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(topic, (frame) => {
          try {
            handler.current?.(JSON.parse(frame.body))
          } catch {
            // A frame we cannot read is not worth breaking the page over.
          }
        })
      },
    })

    client.activate()
    return () => {
      client.deactivate()
    }
  }, [topic])
}
