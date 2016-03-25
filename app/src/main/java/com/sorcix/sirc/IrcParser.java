/*
 * IrcParser.java
 * 
 * This file is part of the Sorcix Java IRC Library (sIRC).
 * 
 * Copyright (C) 2008-2010 Vic Demuzere http://sorcix.com
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.sorcix.sirc;

import android.graphics.Color;

import com.aceft.data.primitives.Emoticon;
import com.aceft.data.primitives.IRCMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 * Parses incoming messages and calls event handlers.
 * 
 * @author Sorcix
 */
final class IrcParser {
	
	/** Buffer for motd. */
	private StringBuffer buffer = null;
	
	/**
	 * Parses normal IRC commands.
	 * 
	 * @param irc IrcConnection receiving this line.
	 * @param line The input line.
	 */
	protected void parseCommand(final IrcConnection irc, final IrcPacket line) {
        if (line.getCommand().contains("color")) {
            String arguments[] = line.getCommand().split(";");

			int color = -1;
			String displayName = "", emotes = "", room_id = "", user_id = "", user_type = "";
			int subscriber = 0, turbo = 0, mod = 0;

			for (String s : arguments) {
				if (s.contains("color=")) {
					s = s.substring(6);
					if (!s.isEmpty())
						color = Color.parseColor(s);
				} else if (s.contains("display-name=")) {
					displayName = s.substring(13);
				} else if (s.contains("emotes=")) {
					emotes = s.substring(7);
				} else if (s.contains("mod=")) {
					mod = Integer.parseInt(s.substring(4));
				} else if (s.contains("room-id=")) {
					room_id = s.substring(8);
				} else if (s.contains("subscriber=")) {
					subscriber = Integer.parseInt(s.substring(11));
				} else if (s.contains("turbo=")) {
					turbo = Integer.parseInt(s.substring(6));
				} else if (s.contains("user-id=")) {
					user_id = s.substring(8);
				} else if (s.contains("user-type=")) {
					user_type = s.substring(10);
				}
			}

//            int color = -1;
//            if (arguments[0].substring(arguments[0].indexOf("=")+1).length() > 1)
//                color = Color.parseColor(arguments[0].substring(arguments[0].indexOf("=")+1));

//            String displayName = arguments[1].substring(arguments[1].indexOf("=")+1);
//            String emotes = arguments[2].substring(arguments[2].indexOf("=")+1);
////            int subscriber = Integer.parseInt(arguments[3].substring(arguments[3].indexOf("=")+1));
//			int subscriber = parseInt(arguments[3], arguments[3].indexOf("=") + 1);
////            int turbo = Integer.parseInt(arguments[4].substring(arguments[4].indexOf("=")+1));
//			int turbo = parseInt(arguments[4], arguments[4].indexOf("=")+1);
//            String userType = arguments[5].substring(arguments[5].indexOf("=")+1);

            int firstIndexMessage = line.getMessage().indexOf(":") + 1;
            String message = line.getMessage().substring(firstIndexMessage, line.getMessage().length());

            ArrayList<Emoticon> listEmotes = new ArrayList<>();
            if (!emotes.equals("")) {
                String sEmotes[] = emotes.split("/");

                if (sEmotes.length > 1) {
                    String asdf = "sadfasdf";
                    asdf.substring(1,2);
                }

                String id = "";


                String sRanges[];
                String firstLast[];

                for (String e : sEmotes) {
                    id = e.substring(0, e.indexOf(":"));
                    sRanges = e.substring(e.indexOf(":")+1, e.length()).split(",");
                    ArrayList<Integer> startIndices = new ArrayList<>();
                    int regexLength= 0;

                    for (String i : sRanges) {
                        firstLast = i.split("-");
                        startIndices.add(parseInt(firstLast[0]));
                        regexLength = parseInt(firstLast[1]) - parseInt(firstLast[0]) + 1;
                    }
                    Emoticon em = new Emoticon(id, regexLength, startIndices);
                    listEmotes.add(new Emoticon(id, regexLength, startIndices));
                }
            }

            if (displayName.equals("")) {
                displayName = line.getMessage().substring(0,line.getMessage().indexOf("!"));
            }


//            line.getMessageText().last
//            @color=#2E8B57;display-name=acefortwitch;emotes=170:0-8,10-18/19:20-30,32-42;subscriber=0;turbo=0;user-type=;user_type= :acefortwitch!acefortwitch@acefortwitch.tmi.twitch.tv PRIVMSG #nl_kripp :DatSheffy DatSheffy PazPazowitz PazPazowitz

//          @color=;display-name=acefortwitch;emotes=973:10-17/15:19-27/170:0-8;subscriber=0;turbo=0;user-type=;user_type= :acefortwitch!acefortwitch@acefortwitch.tmi.twitch.tv PRIVMSG #rotterdam08 :DatSheffy DAESuppy JKanStyle


            final Channel chan = irc.getState().getChannel(line.getArguments());
            for (final Iterator<MessageListener> it = irc.getMessageListeners(); it.hasNext();) {
                it.next().onRichMessage(irc, chan, new IRCMessage(color, displayName, subscriber, turbo, listEmotes, user_type, message));
            }

            int asdfas = 1;

        } else if (line.getCommand().equals("PRIVMSG") && (line.getArguments() != null)) {
			if (line.isCtcp()) {
				// reply to CTCP commands
				if (line.getMessage().startsWith("ACTION ")) {
					if (Channel.CHANNEL_PREFIX.indexOf(line.getArguments().charAt(0)) >= 0) {
						// to channel
						final Channel chan = irc.getState().getChannel(line.getArguments());
						for (final Iterator<MessageListener> it = irc.getMessageListeners(); it.hasNext();) {
							it.next().onAction(irc, chan.updateUser(line.getSender(), true), chan, line.getMessage().substring(7));
						}
					} else {
						// to user
						for (final Iterator<MessageListener> it = irc.getMessageListeners(); it.hasNext();) {
							it.next().onAction(irc, line.getSender(), line.getMessage().substring(7));
						}
					}
				} else if (line.getMessage().equals("VERSION") || line.getMessage().equals("FINGER")) {
					// send custom version string
					line.getSender().sendCtcpReply("VERSION " + irc.getVersion());
				} else if (line.getMessage().equals("SIRCVERS")) {
					// send sIRC version information
					line.getSender().sendCtcpReply("SIRCVERS " + IrcConnection.ABOUT);
				} else if (line.getMessage().equals("TIME")) {
					// send current date&time
					line.getSender().sendCtcpReply(new Date().toString());
				} else if (line.getMessage().startsWith("PING ")) {
					// send ping reply
					line.getSender().sendCtcpReply("PING " + line.getMessage().substring(5), true);
				} else if (line.getMessage().startsWith("SOURCE")) {
					// send sIRC source
					line.getSender().sendCtcpReply("SOURCE http://j-sirc.googlecode.com");
				} else if (line.getMessage().equals("CLIENTINFO")) {
					// send client info
					line.getSender().sendCtcpReply("CLIENTINFO VERSION TIME PING SOURCE FINGER SIRCVERS");
				} else {
					// send error message
					line.getSender().sendCtcpReply("ERRMSG CTCP Command not supported. Use CLIENTINFO to list supported commands.");
				}
			} else if (line.getArguments().startsWith("#") || line.getArguments().startsWith("&")) {
				// to channel
				final Channel chan = irc.getState().getChannel(line.getArguments());
				for (final Iterator<MessageListener> it = irc.getMessageListeners(); it.hasNext();) {
					it.next().onMessage(irc, chan.updateUser(line.getSender(), true), chan, line.getMessage());
				}
			} else {
				// to user
				for (final Iterator<MessageListener> it = irc.getMessageListeners(); it.hasNext();) {
					it.next().onPrivateMessage(irc, line.getSender(), line.getMessage());
				}
			}
		} else if (line.getCommand().equals("NOTICE") && (line.getArguments() != null)) {
			if (line.isCtcp()) {
				// receive CTCP replies.
				final int cmdPos = line.getMessage().indexOf(' ');
				final String command = line.getMessage().substring(0, cmdPos);
				final String args = line.getMessage().substring(cmdPos + 1);
				if (command.equals("VERSION") || command.equals("PING") || command.equals("CLIENTINFO")) {
					for (final Iterator<MessageListener> it = irc.getMessageListeners(); it.hasNext();) {
						it.next().onCtcpReply(irc, line.getSender(), command, args);
					}
				}
			} else if (Channel.CHANNEL_PREFIX.indexOf(line.getArguments().charAt(0)) >= 0) {
				// to channel
				final Channel chan = irc.getState().getChannel(line.getArguments());
				for (final Iterator<MessageListener> it = irc.getMessageListeners(); it.hasNext();) {
					it.next().onNotice(irc, chan.updateUser(line.getSender(), true), chan, line.getMessage());
				}
			} else {
				// to user
				for (final Iterator<MessageListener> it = irc.getMessageListeners(); it.hasNext();) {
					it.next().onNotice(irc, line.getSender(), line.getMessage());
				}
			}
		} else if (line.getCommand().equals("JOIN")) {
			// some server seem to send the joined channel as message,
			// while others have it as an argument. (quakenet related)
			String channel;
			if (line.hasMessage()) {
				channel = line.getMessage();
			} else {
				channel = line.getArguments();
			}
			// someone joined a channel
			if (line.getSender().isUs()) {
				// if the user joining the channel is the client
				// we need to add it to the channel list.
				irc.getState().addChannel(new Channel(channel, irc, true));
			} else {
				// add user to channel list.
				irc.getState().getChannel(channel).addUser(line.getSender());
			}
			for (final Iterator<ServerListener> it = irc.getServerListeners(); it.hasNext();) {
				it.next().onJoin(irc, irc.getState().getChannel(channel), line.getSender());
			}
		} else if (line.getCommand().equals("PART")) {
			// someone left a channel
			if (line.getSender().isUs()) {
				// if the user leaving the channel is the client
				// we need to remove it from the channel list
				irc.getState().removeChannel(line.getArguments());
				// run garbage collection
				irc.garbageCollection();
			} else {
				// remove user from channel list.
				irc.getState().getChannel(line.getArguments()).removeUser(line.getSender());
			}
			for (final Iterator<ServerListener> it = irc.getServerListeners(); it.hasNext();) {
				it.next().onPart(irc, irc.getState().getChannel(line.getArguments()), line.getSender(), line.getMessage());
			}
		} else if (line.getCommand().equals("QUIT")) {
			// someone quit the IRC server
			final User quitter = line.getSender();
			for (final Iterator<ServerListener> it = irc.getServerListeners(); it.hasNext();) {
				it.next().onQuit(irc, quitter, line.getMessage());
			}
			for (final Iterator<Channel> it = irc.getState().getChannels(); it.hasNext();) {
				final Channel channel = it.next();
				if (channel.hasUser(quitter)) {
					channel.removeUser(quitter);
				}
			}
		} else if (line.getCommand().equals("KICK")) {
			// someone was kicked from a channel
			final String[] data = line.getArgumentsArray();
			final User kicked = new User(data[1], irc);
			final Channel channel = irc.getState().getChannel(data[0]);
			if (kicked.isUs()) {
				// if the user leaving the channel is the client
				// we need to remove it from the channel list
				irc.getState().removeChannel(data[0]);
			} else {
				// remove user from channel list.
				channel.removeUser(kicked);
			}
			for (final Iterator<ServerListener> it = irc.getServerListeners(); it.hasNext();) {
				it.next().onKick(irc, channel, line.getSender(), kicked, line.getMessage());
			}
		} else if (line.getCommand().equals("MODE")) {
			this.parseMode(irc, line);
		} else if (line.getCommand().equals("TOPIC")) {
			// someone changed the topic.
			for (final Iterator<ServerListener> it = irc.getServerListeners(); it.hasNext();) {
				final Channel chan = irc.getState().getChannel(line.getArguments());
				it.next().onTopic(irc, chan, chan.updateUser(line.getSender(), false), line.getMessage());
			}
		} else if (line.getCommand().equals("NICK")) {
			User newUser;
			if (line.hasMessage()) {
				newUser = new User(line.getMessage(), irc);
			} else {
				newUser = new User(line.getArguments(), irc);
			}
			// someone changed his nick
			for (final Iterator<Channel> it = irc.getState().getChannels(); it.hasNext();) {
				it.next().renameUser(line.getSender().getNickLower(), newUser.getNick());
			}
			// change local user
			if (line.getSender().isUs()) {
				irc.getState().getClient().setNick(newUser.getNick());
			}
			for (final Iterator<ServerListener> it = irc.getServerListeners(); it.hasNext();) {
				it.next().onNick(irc, line.getSender(), newUser);
			}
		} else if (line.getCommand().equals("INVITE")) {
			// someone was invited
			final String[] args = line.getArgumentsArray();
			if ((args.length >= 2) && (line.getMessage() == null)) {
				final Channel channel = irc.createChannel(args[1]);
				for (final Iterator<ServerListener> it = irc.getServerListeners(); it.hasNext();) {
					it.next().onInvite(irc, line.getSender(), new User(args[0], irc), channel);
				}
			}
		} else {
			if (irc.getAdvancedListener() != null) {
				irc.getAdvancedListener().onUnknown(irc, line);
			}
		}
	}

	private void parseMode(final IrcConnection irc, final IrcPacket line) {
		final String[] args = line.getArgumentsArray();
		if ((args.length >= 2) && (Channel.CHANNEL_PREFIX.indexOf(args[0].charAt(0)) >= 0)) {
			// general mode event listener
			for (final Iterator<ServerListener> it = irc.getServerListeners(); it.hasNext();) {
				it.next().onMode(irc, irc.getState().getChannel(args[0]), line.getSender(), line.getArguments().substring(args[0].length() + 1));
			}
			if ((args.length >= 3)) {
				final Channel channel = irc.getState().getChannel(args[0]);
				final String mode = args[1];
				final boolean enable = mode.charAt(0) == '+';
				char current;
				// tries all known modes.
				// this is an ugly part of sIRC, but the only way to
				// do this.
				for (int x = 2; x < args.length; x++) {
					current = mode.charAt(x - 1);
					if (current == User.MODE_ADMIN) {
						// admin or deadmin
						if (enable) {
							for (final Iterator<ModeListener> it = irc.getModeListeners(); it.hasNext();) {
								it.next().onAdmin(irc, channel, line.getSender(), irc.createUser(args[x]));
							}
						} else {
							for (final Iterator<ModeListener> it = irc.getModeListeners(); it.hasNext();) {
								it.next().onDeAdmin(irc, channel, line.getSender(), irc.createUser(args[x]));
							}
						}
					} else if (current == User.MODE_OPERATOR) {
						// op or deop
						if (enable) {
							for (final Iterator<ModeListener> it = irc.getModeListeners(); it.hasNext();) {
								it.next().onOp(irc, channel, line.getSender(), irc.createUser(args[x]));
							}
						} else {
							for (final Iterator<ModeListener> it = irc.getModeListeners(); it.hasNext();) {
								it.next().onDeOp(irc, channel, line.getSender(), irc.createUser(args[x]));
							}
						}
					} else if (current == User.MODE_HALF_OP) {
						// halfop or dehalfop
						if (enable) {
							for (final Iterator<ModeListener> it = irc.getModeListeners(); it.hasNext();) {
								it.next().onHalfop(irc, channel, line.getSender(), irc.createUser(args[x]));
							}
						} else {
							for (final Iterator<ModeListener> it = irc.getModeListeners(); it.hasNext();) {
								it.next().onDeHalfop(irc, channel, line.getSender(), irc.createUser(args[x]));
							}
						}
					} else if (current == User.MODE_FOUNDER) {
						// founder or defounder
						if (enable) {
							for (final Iterator<ModeListener> it = irc.getModeListeners(); it.hasNext();) {
								it.next().onFounder(irc, channel, line.getSender(), irc.createUser(args[x]));
							}
						} else {
							for (final Iterator<ModeListener> it = irc.getModeListeners(); it.hasNext();) {
								it.next().onDeFounder(irc, channel, line.getSender(), irc.createUser(args[x]));
							}
						}
					}
				}
			}
		}
	}

	protected void parseNumeric(final IrcConnection irc, final IrcPacket line) {
		switch (line.getNumericCommand()) {
			case IrcPacket.RPL_TOPIC:
				for (final Iterator<ServerListener> it = irc.getServerListeners(); it.hasNext();) {
					it.next().onTopic(irc, irc.getState().getChannel(line.getArgumentsArray()[1]), null, line.getMessage());
				}
				break;
			case IrcPacket.RPL_NAMREPLY:
//				final String[] arguments = line.getArgumentsArray();
//				final Channel channel = irc.getState().getChannel(arguments[arguments.length - 1]);
//				if (channel != null) {
//					final String[] users = line.getMessage().split(" ");
//					User buffer;
//					for (final String user : users) {
//						buffer = new User(user, irc);
//						channel.updateUser(buffer, true);
//					}
//				}
				break;
			case IrcPacket.RPL_MOTD:
				if (this.buffer == null) {
					this.buffer = new StringBuffer();
				}
				this.buffer.append(line.getMessage());
				this.buffer.append(IrcConnection.ENDLINE);
				break;
			case IrcPacket.RPL_ENDOFMOTD:
				if (this.buffer != null) {
					final String motd = this.buffer.toString();
					this.buffer = null;
					for (final Iterator<ServerListener> it = irc.getServerListeners(); it.hasNext();) {
						it.next().onMotd(irc, motd);
					}
				}
				break;
			case IrcPacket.RPL_BOUNCE:
				// redirect to another server.
				if (irc.isBounceAllowed()) {
					irc.disconnect();
					irc.setServer(new IrcServer(line.getArgumentsArray()[0], line.getArgumentsArray()[1]));
					try {
						irc.connect();
					} catch (final Exception ex) {
						// TODO: exception while connecting to new
						// server?
					}
				}
				break;
            case IrcPacket.RPL_ENDOFNAMES:
//                final String[] arguments2 = line.getArgumentsArray();
//                final Channel channel2 = irc.getState().getChannel(arguments2[arguments2.length - 1]);
//                final Iterator<User> users = channel2.getUsers();
//                for (final Iterator<ServerListener> it = irc.getServerListeners(); it.hasNext(); ) {
//                    it.next().onNamesComplete(irc, channel2, users);
//                    }
                break;
			default:
				if (irc.getAdvancedListener() != null) {
					irc.getAdvancedListener().onUnknown(irc, line);
				}
		}
	}

	private int parseInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch (Exception ignored) {
			return 0;

		}
	}

	private int parseInt(String s, int a) {
		try {
			return Integer.parseInt(s.substring(a));
		} catch (Exception ignored) {
			return 0;

		}
	}
}
