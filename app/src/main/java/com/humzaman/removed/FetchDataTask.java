package com.humzaman.removed;

/*
public class FetchDataTask extends AsyncTask<String, Void, List<String>> {
    private static final String TAG = "FetchDataTask";
    private WeakReference<Activity> activity;

    public FetchDataTask(WeakReference<Activity> activity) {
        this.activity = activity;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.i(TAG, "onPreExecute: Showing ProgressDialog");
        progressDialog = new ProgressDialog(activity);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Unremoving...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setProgress(0);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cancel(true);
                finish();
            }
        });
        progressDialog.show();
    }

    @Override
    protected List<String> doInBackground(String... params) {
        Log.i(TAG, "doInBackground() Fetching data.");
        List<String> result = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        HttpURLConnection urlConnectionScore = null;

        try {
            URL url = new URL(params[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            int pushshiftResponseCode = urlConnection.getResponseCode();

            if (pushshiftResponseCode == HttpURLConnection.HTTP_OK) {
                Log.i(TAG, "Response from Pushshift: " + pushshiftResponseCode);
                InputStream inputStream = urlConnection.getInputStream();

                // convert inputStream to string
                if (inputStream != null) {
                    result.add(convertInputStreamToString(inputStream)); // 0
                    Log.i(TAG, "Pushshift data received: " + result.get(0));

                    // get current comment score from reddit
                    URL urlScore = new URL("https://api.reddit.com/api/info/?id=t1_" + id);
                    urlConnectionScore = (HttpURLConnection) urlScore.openConnection();
                    int redditResponseCode = urlConnectionScore.getResponseCode();

                    if (redditResponseCode == HttpURLConnection.HTTP_OK) {
                        Log.i(TAG, "Response from reddit: " + redditResponseCode);
                        InputStream inputStreamScore = urlConnectionScore.getInputStream();

                        if (inputStreamScore != null) {
                            result.add(convertInputStreamToString(inputStreamScore)); // 1
                            Log.i(TAG, "reddit data received: " + result.get(1));
                        }
                        else {
                            Log.e(TAG, "Failed to retrieve data from reddit.");
                            result.add("FAILED"); // 1
                        }
                    }
                    else {
                        Log.e(TAG, "Response from reddit: " + redditResponseCode);
                        result.add("FAILED"); // 1
                    }
                }
                else {
                    Log.e(TAG, "Failed to retrieve data from Pushshift.");
                    result.add("FAILED"); // 0
                }

                return result;
            }
            else {
                Log.e(TAG, "Response from Pushshift: " + pushshiftResponseCode);
                result.add("ERROR"); // 0
                result.add(String.valueOf(pushshiftResponseCode)); // 1
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }

            if (urlConnectionScore != null) {
                urlConnectionScore.disconnect();
            }
        }

        Log.e(TAG, "Url connection error");
        return null;
    }

    @Override
    protected void onPostExecute(List<String> dataFetched) {
        Log.i(TAG, "onPostExecute()");

        if (dataFetched == null) {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
            displayAlert(ResultCode.NO_INTERNET, null);
        }
        else if (dataFetched.get(0).equals("FAILED")) {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
            displayAlert(ResultCode.FAILED, null);
        }
        else if (dataFetched.get(0).equals("ERROR")) {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
            displayAlert(ResultCode.ERROR_RESPONSE, dataFetched.get(1));
        }
        else {
            //parse the JSON data and then display
            parseJSON(dataFetched);

            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            if (parsedData.size() != 0)
                displayAlert(ResultCode.VALID_COMMENT, null);
            else {
                Log.e(TAG, "No data found on pushshift.");
                displayAlert(ResultCode.NO_DATA_FOUND, null);
            }
        }
    }

    private String convertInputStreamToString (InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder result = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null)
            result.append(line);

        inputStream.close();
        return result.toString();
    }

    private void parseJSON(List<String> data) {
        try {
            JSONArray pushshiftDataArray = (new JSONObject(data.get(0))).getJSONArray("data");
            Log.i(TAG, "Pushshift data array: " + pushshiftDataArray.toString());

            parsedData = new ArrayList<>();

            if (pushshiftDataArray.length() > 0) {
                JSONObject pushshiftObject = pushshiftDataArray.getJSONObject(0);

                parsedData.add(pushshiftObject.has("author") ? pushshiftObject.getString("author") : "null");                   // 0
                parsedData.add(pushshiftObject.has("body") ? pushshiftObject.getString("body") : "null");                       // 1
                parsedData.add(pushshiftObject.has("score") ? pushshiftObject.getString("score") : "null");                     // 2
                parsedData.add(pushshiftObject.has("id") ? pushshiftObject.getString("id") : "null");                           // 3
                parsedData.add(pushshiftObject.has("permalink") ?                                                                     // 4
                        "https://www.reddit.com" + pushshiftObject.getString("permalink") : intentString);
                parsedData.add(pushshiftObject.has("created_utc") ? pushshiftObject.getString("created_utc") : "null");         // 5
                parsedData.add(pushshiftObject.has("retrieved_on") ? pushshiftObject.getString("retrieved_on") : "null");       // 6
                parsedData.add(pushshiftObject.has("subreddit") ? pushshiftObject.getString("subreddit") : "null");             // 7
                parsedData.add(pushshiftObject.has("subreddit_id") ? pushshiftObject.getString("subreddit_id") : "null");       // 8
                parsedData.add(pushshiftObject.has("link_id") ? pushshiftObject.getString("link_id") : "null");                 // 9
                parsedData.add(pushshiftObject.has("parent_id") ? pushshiftObject.getString("parent_id") : "null");             // 10
                parsedData.add(pushshiftObject.has("author_fullname") ? pushshiftObject.getString("author_fullname") : "null"); // 11
                Log.i(TAG, "Parsed Pushshift data: " + parsedData.toString());

                // current comment score from reddit
                if (!data.get(1).equals("FAILED")) {
                    JSONObject redditObject = (new JSONObject(data.get(1))).getJSONObject("data").getJSONArray("children").getJSONObject(0).getJSONObject("data");
                    String score = redditObject.getString("score");

                    Log.i(TAG, "Score from reddit: " + score);
                    parsedData.set(2, score);
                }
            }
        } catch(Exception e) {
            Log.e(TAG, "Error parsing data: " + e.getMessage());
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.i(TAG, "FetchDataTask canceled.");
    }

}
*/
