{-# LANGUAGE OverloadedStrings #-}
import Control.Monad (forM_, forever)
import Data.Text (Text)
import Control.Concurrent (MVar, newMVar, modifyMVar_, readMVar)
import Control.Monad.IO.Class (liftIO)
import Network.HTTP.Types.URI (decodePathSegments)

import qualified Network.WebSockets as WS

type Member  = (Text, WS.Connection)
type Members = [Member]

broadcast :: Text -> Text -> Members -> IO ()
broadcast room message members = do
  forM_ members $ \(r, conn) -> do
    if r == room
      then WS.sendTextData conn message
      else return ()

application :: MVar Members -> WS.ServerApp
application state pending = do
    conn <- WS.acceptRequest pending
    liftIO $ modifyMVar_ state $ \s -> do
      let s' = (room, conn) : s
      return s'
    forever $ do
      msg <- WS.receiveData conn
      liftIO $ readMVar state >>= broadcast room msg
  where
    req = WS.pendingRequest pending
    room = head $ decodePathSegments (WS.requestPath req)

main :: IO ()
main = do
  state <- newMVar []
  WS.runServer "127.0.0.1" 9000 $ application state
