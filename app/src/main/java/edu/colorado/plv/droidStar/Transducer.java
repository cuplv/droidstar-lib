package edu.colorado.plv.droidStar;

import java.util.Queue;
import java.util.ArrayDeque;
import java.util.concurrent.CountDownLatch;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;


import android.content.Intent;
import android.content.Context;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import static edu.colorado.plv.droidStar.Static.*;

public class Transducer {

    private Context context;
    private SpeechRecognizerLP purpose;
    private Queue<String> inputs;
    private Queue<String> outputs;
    private int queryNum;
    private CountDownLatch outputReceived;

    Transducer(Context c) {
        this.context = c;
        this.purpose = new SpeechRecognizerLP(context);
        this.inputs = new ArrayDeque();
        this.outputs = new ArrayDeque();
        this.queryNum = 0;
        // this.outputReceived = new CountDownLatch(1);
    }

    private synchronized void advance() {
        outputs.clear();
        queryNum++;
    }

    public void reset() {
        purpose.reset();
        inputs.clear();
        advance();
    }

    public void rollback() {
        purpose.reset();
        playAll(inputs);
        advance();
    }

    private void playAll(Queue<String> is) {
        // perform all inputs in sequence, discarding outputs
    }

    public void reportOutput(String o, int q) {
        if (q == queryNum) {
            log("Logging ouput \"" + o + "\" in queue.");
            outputs.add(o);
            outputReceived.countDown();
        } else {
            log("Output with stale queryNum ignored.");
        }
    }

    public String query(String i) {
        if (i == DELTA) {
            String output = outputs.poll();
            if (output == null) {
                return BETA;
            } else {
                return output;
            }
        } else {
            // perform action and then peek at queue to see if it
            // produced an error
            log("Giving input \"" + i + "\"...");
            advance();
            outputReceived = new CountDownLatch(1);
            purpose.giveInput(new OutputCallback(queryNum), i);
            try {outputReceived.await();} catch (Exception e) {log("Interrupted await?");}

            log("Output returned, inspecting for error...");
            if (SpeechRecognizerLP.isError(outputs.peek())) {
                rollback();
                return REJECTED;
            } else {
                return ACCEPTED;
            }
        }
    }

    // public void multiQuery(Queue<String> is) {
    //     final Queue<String> inputs = is;
    //     final String input = inputs.poll();
    //     Callback c = new Callback() {
    //             public boolean handleMessage(Message output) {
    //                 log("Got back " + output.obj + ", continuing...");
    //                 multiQuery(inputs);
    //                 return true;
    //             }
    //         };
    //     if (input != null) {
    //         log("Giving input...");
    //         purpose.giveInput(c, input);
    //     } else {
    //         log("Reached end of inputs");
    //     }
    // }

    private class OutputCallback implements Callback {
        private int onQueryNum;

        OutputCallback(int n) {
            this.onQueryNum = n;
        }

        public boolean handleMessage(Message o) {
            String output = o.getData().getString("output");
            log("Got back " + output + ", continuing...");
            reportOutput(output, onQueryNum);
            return true;
        }
    }
}