package com.aceft.data;

import android.util.Log;

import com.aceft.data.primitives.IRCMessage;
import com.aceft.ui_fragments.channel_fragments.ChatFragment;
import com.sorcix.sirc.AdvancedListener;
import com.sorcix.sirc.Channel;
import com.sorcix.sirc.IrcConnection;
import com.sorcix.sirc.IrcPacket;
import com.sorcix.sirc.MessageListener;
import com.sorcix.sirc.NickNameException;
import com.sorcix.sirc.PasswordException;
import com.sorcix.sirc.ServerListener;
import com.sorcix.sirc.User;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;

public class TwitchChatClient implements MessageListener, AdvancedListener, ServerListener {
    private boolean isSender;
    private ArrayList<String> mServers;
    private ChatFragment mFragment;
    private String PASS = "oauth:0gmbfdy11inwo69fef5ljplt3yeth4";
    private String NICK = "acefortwitch";
    private IrcConnection ircConnection;
    private TwitchChatClient client;
    private Channel mJoinedChannel;

    public TwitchChatClient(ChatFragment chatFragment, String nick, String token, ArrayList<String> servers) {
        this(chatFragment, nick, token, servers, false);
    }

    public TwitchChatClient(ChatFragment chatFragment, String nick, String token, ArrayList<String> servers, boolean sender) {
        mFragment = chatFragment;
        client = this;
        NICK = nick;
        PASS = token;
        mServers = servers;
        this.isSender = sender;
    }

    public void connect(final String channel, final int retry) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (retry >= mServers.size()-1) {
                        if (mFragment.getActivity() == null) return;
                        mFragment.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mFragment.chatIsOffline();
                            }
                        });
                        return;
                    }

                    String address = getServerAddress(mServers.get(retry));
                    int port = getServerPort(mServers.get(retry));

                    if (!serverIsOnline(address, port, 2000)) {
                        connect(channel, retry + 1);
                        return;
                    }

                    ircConnection = new IrcConnection(address, port, PASS);
                    ircConnection.setNick(NICK);
                    ircConnection.setAdvancedListener(client);
                    ircConnection.addServerListener(client);
                    ircConnection.addMessageListener(client);
                    ircConnection.connect();

                    if (!ircConnection.isConnected()) {
                        connect(channel, retry + 1);
                        return;
                    }

                    Channel summit = ircConnection.createChannel(channel);
                    summit.join();
                } catch (IOException | NickNameException | PasswordException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void joinChannel(ChatFragment chatFragment, String channel) {
        if (chatFragment == null) return;
        mFragment = chatFragment;
        partChannel();
        connect(channel, 0);
    }

    public void sendMessage(String message) {
        if(mJoinedChannel == null || message == null) return;
            mJoinedChannel.sendMessage(message);
    }

    public void partChannel() {
        if(mJoinedChannel == null) return;
        mJoinedChannel.part();
        if(ircConnection == null) return;
        ircConnection.disconnect();
    }

    ////////////////////// MessageListener Part //////////////////////////////////////////////
    @Override
    public void onAction(IrcConnection irc, User sender, Channel target, String action) {
    }

    @Override
    public void onAction(IrcConnection irc, User sender, String action) {
    }

    @Override
    public void onCtcpReply(IrcConnection irc, User sender, String command, String message) {
    }

    @Override
    public void onRichMessage(IrcConnection irc, Channel target, final IRCMessage message) {
        if (isSender) return;
        if (mFragment != null && mFragment.getActivity() != null) {
            mFragment.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFragment.newMessage(message);
                }
            });
        }
    }

    @Override
    public void onMessage(IrcConnection irc, final User sender, Channel target, final String message) {
//        if (mFragment != null && mFragment.getActivity() != null) {
//            mFragment.getActivity().runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    mFragment.newMessage(new IRCMessage(isSender.getNick(), message));
//                }
//            });
//        }
    }

    @Override
    public void onNotice(IrcConnection irc, User sender, Channel target, String message) {
    }

    @Override
    public void onNotice(IrcConnection irc, User sender, String message) {
    }

    @Override
    public void onPrivateMessage(IrcConnection irc, User sender, String message) {
    }

    //////////////////////////// ServerListener Part ////////////////////////////////////////////
    @Override
    public void onConnect(IrcConnection irc) {
//        Log.d("onConnect", irc.getServerAddress());
    }

    @Override
    public void onDisconnect(IrcConnection irc) {
//        Log.d("onDisconnect", irc.getServerAddress());
    }

    @Override
    public void onInvite(IrcConnection irc, User sender, User user, Channel channel) {
    }

    @Override
    public void onJoin(IrcConnection irc, Channel channel, User user) {
        mJoinedChannel = channel;
        if (isSender) return;
        mJoinedChannel.sendRaw("CAP REQ :twitch.tv/tags");
        if (mFragment != null && mFragment.getActivity() != null) {
            mFragment.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFragment.onJoined();
                }
            });
        }
    }

    @Override
    public void onKick(IrcConnection irc, Channel channel, User sender, User user, String msg) {
    }

    @Override
    public void onNamesComplete(IrcConnection irc, Channel channel, Iterator<User> users) {
    }

    @Override
    public void onMode(IrcConnection irc, Channel channel, User sender, String mode) {
    }

    @Override
    public void onMotd(IrcConnection irc, String motd) {
    }

    @Override
    public void onNick(IrcConnection irc, User oldUser, User newUser) {
    }

    @Override
    public void onPart(IrcConnection irc, Channel channel, User user, String message) {
    }

    @Override
    public void onQuit(IrcConnection irc, User user, String message) {
    }

    @Override
    public void onTopic(IrcConnection irc, Channel channel, User sender, String topic) {
    }

    //////////////////////////// AdvancedListener Part ////////////////////////////////////////////
    @Override
    public void onUnknown(IrcConnection irc, IrcPacket line) {
        if (isSender) return;
//        Log.d("onUnknown", line.getArguments() + "\n" + line.toString());
    }

    @Override
    public void onUnknownReply(IrcConnection irc, IrcPacket line) {
        if (isSender) return;
//        Log.d("onUnknownReply", line.getArguments() + "\n" + line.toString());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean serverIsOnline(String address, int port, int timeout) {
        SocketAddress socketAddress = new InetSocketAddress(address, port);
        Socket socket = new Socket();
        boolean online = true;
        try {
            socket.connect(socketAddress, timeout);
        } catch (IOException iOException) {
            online = false;
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        return online;
    }

    private String getServerAddress(String s) {
        return s.substring(0, s.indexOf(":"));
    }

    private int getServerPort(String s) {
        String p = s.substring(s.indexOf(":")+1);
        return Integer.parseInt(p);
    }
}
