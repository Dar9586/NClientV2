package com.dar.nclientv2.components;

import androidx.appcompat.app.AppCompatActivity;

import com.dar.nclientv2.settings.Global;

public abstract class ThreadAsyncTask<Params, Progress, Result> {

    private final AppCompatActivity activity;
    private Thread thread;
    public ThreadAsyncTask(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void execute(Params... params) {
        thread = new AsyncThread(params);
        thread.start();
    }

    protected void onPreExecute() {
    }

    protected void onPostExecute(Result result) {
    }

    protected void onProgressUpdate(Progress... values) {
    }

    protected abstract Result doInBackground(Params... params);

    protected final void publishProgress(Progress... values) {
        if (!Global.isDestroyed(activity))
            activity.runOnUiThread(() -> onProgressUpdate(values));
    }

    class AsyncThread extends Thread {

        Params[] params;

        AsyncThread(Params[] params) {
            this.params = params;
        }

        @Override
        public void run() {
            if (!Global.isDestroyed(activity))
                activity.runOnUiThread(ThreadAsyncTask.this::onPreExecute);
            Result result = doInBackground(params);
            if (!Global.isDestroyed(activity))
                activity.runOnUiThread(() -> onPostExecute(result));
        }
    }

}
