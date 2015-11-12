package com.aceft.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.custom_layouts.EmoteTarget;
import com.aceft.data.primitives.Emoticon;
import com.aceft.data.primitives.IRCMessage;
import com.aceft.ui_fragments.channel_fragments.ChatFragment;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;


public class IRCAdapter extends RecyclerView.Adapter<IRCAdapter.ViewHolder> implements EmoteTarget.OnEmoteLoaded {
    private ArrayList<IRCMessage> mIRCMessages;
    private HashMap<String, Integer> mUserColors;
    private Context mContext;
    private RecyclerView recyclerView;
    private int mTheme;
    private int textSizeInSp, textSizeInPx;
    private boolean mAutoScroll = true;
    private float EMOTI_SCALE = 1.8f;
    private float BADGE_SCALE = 1.2f;
    private Bitmap subBadge;
    private OnMessageClicked mMessageClicked;

    public interface OnMessageClicked {
        void nameClicked(String s);
    }

    public void setOnNameClickedListener(ChatFragment cF) {
        mMessageClicked = cF;
    }

    public IRCAdapter(Activity c, RecyclerView r) {
        this(c, r, 0);
    }

    public IRCAdapter(Activity c, RecyclerView r, int theme) {
        mContext = c;
        mTheme = theme;
        if (mIRCMessages == null) mIRCMessages = new ArrayList<>();
        if (mUserColors == null) mUserColors = new HashMap<>();
        recyclerView = r;
    }

    public void update(IRCMessage ircMessage) {
        if (ircMessage == null) return;

        if (ircMessage.getColor() == -1) {
            if (mUserColors.containsKey(ircMessage.getDisplayName()))
                ircMessage.setColor(mUserColors.get(ircMessage.getDisplayName()));
            else {
                ircMessage.setColor(randChatColor());
                mUserColors.put(ircMessage.getDisplayName(), ircMessage.getColor());
            }
        }

//        mIRCMessages.add(ircMessage);
        getSpan(ircMessage);

        if (mIRCMessages.size() > 2000) {
            mIRCMessages = new ArrayList<>(mIRCMessages.subList(1000, mIRCMessages.size()));
            notifyDataSetChanged();
        }
    }

    public void updateView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public void setAutoScroll(boolean scroll) {
        mAutoScroll = scroll;
    }

    public void updateSubBadge(Bitmap b) {
        if (b == null) return;
        float scale = 1.0f * b.getWidth() / b.getHeight();
        b = Bitmap.createScaledBitmap(b,(int) (BADGE_SCALE * textSizeInPx * scale) , (int) (BADGE_SCALE * textSizeInPx), true);
        subBadge = b;
        notifyDataSetChanged();
    }

    public void updateTheme(int i) {
        mTheme = i;
        notifyDataSetChanged();
    }

    @Override
    public void emoteLoaded(Bitmap b, String id, IRCMessage ircMessage) {
        float scale = 1.0f * b.getWidth() / b.getHeight();
        b = Bitmap.createScaledBitmap(b,(int) (EMOTI_SCALE * textSizeInPx * scale) , (int) (EMOTI_SCALE * textSizeInPx), true);
        ircMessage.setBitmap(b, id);
        if (ircMessage.emotesReady())
            parseAndShow(ircMessage);
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView message;

        public ViewHolder(View itemView) {
            super(itemView);
            this.message = (TextView) itemView.findViewById(R.id.textChatMessage);
            this.message.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeInSp);

            if (mTheme == 1)
                this.message.setTextColor(mContext.getResources().getColor(R.color.chat_white));
        }
    }

    @Override
    public IRCAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layout_chat, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(IRCAdapter.ViewHolder holder, int position) {

        if (mIRCMessages.get(position).getFormattedText() == null) {
            holder.message.setText("");
        } else
            holder.message.setText(mIRCMessages.get(position).getFormattedText());

        holder.message.setMovementMethod(LinkMovementMethod.getInstance());
    }


    private void getSpan(final IRCMessage ircMessage) {
        for (Emoticon e : ircMessage.getEmotes()) {
            Picasso.with(mContext)
                    .load(e.getUrl())
                    .into(new EmoteTarget(this, e.getId(), ircMessage));
        }

        if (ircMessage.getEmotes().size() == 0)
            parseAndShow(ircMessage);
    }


    private void parseAndShow(final IRCMessage ircMessage) {
        String sender = ircMessage.getDisplayName();
        Spanned message = new SpannableString("");

        SpannableString spanS = new SpannableString(sender);
        ClickableSpan cSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                if (mMessageClicked != null)
                    mMessageClicked.nameClicked(ircMessage.getDisplayName());
            }
            @Override public void updateDrawState(TextPaint ds){
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
                ds.setColor(ircMessage.getColor());
            }
        };
        spanS.setSpan(cSpan, 0, sender.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        spanS.setSpan(new StyleSpan(Typeface.BOLD),0 ,sender.length(), 0);
//        spanS.setSpan(new ForegroundColorSpan(ircMessage.getColor()), 0 ,sender.length(), 0);

        SpannableString spanM = new SpannableString(ircMessage.getMessageText());
        ImageSpan span;
        int regexLength;
        for (Emoticon e : ircMessage.getEmotes()) {
            regexLength = e.getRegexLength();
            for (int i : e.getStartIndices()) {
                span = new ImageSpan(mContext, e.getEmoti(), ImageSpan.ALIGN_BOTTOM);
                try {
                    spanM.setSpan(span, i, i+regexLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } catch (IndexOutOfBoundsException ignored) {
                }
            }
        }

        if (ircMessage.getSubscriber() == 1) {
            SpannableString spanSub = new SpannableString(" ");
            span = new ImageSpan(mContext, subBadge, ImageSpan.ALIGN_BOTTOM);
            spanSub.setSpan(span, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            message = (Spanned) TextUtils.concat(spanSub, message, " ");
        }

        if (ircMessage.getTurbo() == 1) {
            SpannableString spanTurbo = new SpannableString(" ");
            Bitmap b = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.chat_turbo);
            b = Bitmap.createScaledBitmap(b, (int) (textSizeInPx * BADGE_SCALE),  (int) (textSizeInPx * BADGE_SCALE), false);
            span = new ImageSpan(mContext, b, ImageSpan.ALIGN_BOTTOM);
            spanTurbo.setSpan(span, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            message = (Spanned) TextUtils.concat(message, spanTurbo, " ");
        }

        if (ircMessage.getUserType().equals("mod")) {
            SpannableString spanMod = new SpannableString(" ");
            Bitmap b = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.chat_mod);
            b = Bitmap.createScaledBitmap(b, (int) (textSizeInPx * BADGE_SCALE),  (int) (textSizeInPx * BADGE_SCALE), false);
            span = new ImageSpan(mContext, b, ImageSpan.ALIGN_BOTTOM);
            spanMod.setSpan(span, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            message = (Spanned) TextUtils.concat(spanMod, " ", message);
        }

        message = (Spanned) TextUtils.concat(message, spanS, ": ", spanM);

        ircMessage.setFormattedText(message);
        mIRCMessages.add(ircMessage);
        notifyItemInserted(mIRCMessages.size() - 1);
        if (mAutoScroll)
            recyclerView.scrollToPosition(mIRCMessages.size() - 1);
    }

    private void parseAndShowThreaded(final IRCMessage ircMessage) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String sender = ircMessage.getDisplayName();
                Spanned message = new SpannableString("");

                SpannableString spanS = new SpannableString(sender);
                spanS.setSpan(new StyleSpan(Typeface.BOLD),0 ,sender.length(), 0);
                spanS.setSpan(new ForegroundColorSpan(ircMessage.getColor()), 0 ,sender.length(), 0);

                SpannableString spanM = new SpannableString(ircMessage.getMessageText());
                ImageSpan span;
                int regexLength;
                Bitmap bE;
                for (Emoticon e : ircMessage.getEmotes()) {
                    regexLength = e.getRegexLength();
                    for (int i : e.getStartIndices()) {
                        bE = e.getEmoti();
                        float scale = 1.0f * bE.getWidth() / bE.getHeight();
                        bE = Bitmap.createScaledBitmap(bE,(int) (EMOTI_SCALE * textSizeInPx * scale) , (int) (EMOTI_SCALE * textSizeInPx), true);
                        span = new ImageSpan(mContext, bE, ImageSpan.ALIGN_BOTTOM);
                        spanM.setSpan(span, i, i+regexLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }

                if (ircMessage.getSubscriber() == 1) {
                    SpannableString spanSub = new SpannableString(" ");
                    span = new ImageSpan(mContext, subBadge, ImageSpan.ALIGN_BOTTOM);
                    spanSub.setSpan(span, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    message = (Spanned) TextUtils.concat(spanSub, message, " ");
                }

                if (ircMessage.getTurbo() == 1) {
                    SpannableString spanTurbo = new SpannableString(" ");
                    Bitmap b = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.chat_turbo);
                    b = Bitmap.createScaledBitmap(b, (int) (textSizeInPx * BADGE_SCALE),  (int) (textSizeInPx * BADGE_SCALE), false);
                    span = new ImageSpan(mContext, b, ImageSpan.ALIGN_BOTTOM);
                    spanTurbo.setSpan(span, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    message = (Spanned) TextUtils.concat(message, spanTurbo, " ");
                }

                if (ircMessage.getUserType().equals("mod")) {
                    SpannableString spanMod = new SpannableString(" ");
                    Bitmap b = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.chat_mod);
                    b = Bitmap.createScaledBitmap(b, (int) (textSizeInPx * BADGE_SCALE),  (int) (textSizeInPx * BADGE_SCALE), false);
                    span = new ImageSpan(mContext, b, ImageSpan.ALIGN_BOTTOM);
                    spanMod.setSpan(span, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    message = (Spanned) TextUtils.concat(spanMod, " ", message);
                }

                message = (Spanned) TextUtils.concat(message, spanS, ": ", spanM);

                final IRCMessage fMessage = new IRCMessage(ircMessage, message);
//                ircMessage.setFormattedText(message);

                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mIRCMessages.add(fMessage);
                        notifyItemInserted(mIRCMessages.size() - 1);
                        if (mAutoScroll)
                            recyclerView.scrollToPosition(mIRCMessages.size() - 1);
                    }
                });
            }
        }).start();
    }

    public ArrayList<IRCMessage> getMessages() {
        return mIRCMessages;
    }

    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return mIRCMessages.size();
    }

    public void setTextSize(int textSize) {
        if (mContext == null) return;
        this.textSizeInSp = textSize;
        this.textSizeInPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, mContext.getResources().getDisplayMetrics());
        notifyDataSetChanged();
    }

    private int randChatColor() {
        int rand = (int) (Math.random()*14);
        String s = mContext.getResources().getStringArray(R.array.chat_colors)[rand];
        return Color.parseColor(s);
    }
}