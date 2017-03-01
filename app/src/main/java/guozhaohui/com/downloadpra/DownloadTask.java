package guozhaohui.com.downloadpra;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Created by ${GuoZhaoHui} on 2017/2/27.
 * email:guozhaohui628@gmail.com
 */

/**
 * AsyncTask<params,progress,result>
 * params:在使用asynctask时需要传入的参数，可以供后台任务使用，可以为void
 * progress：后台任务执行时，如果需要在界面上显示当前进度，则使用这里指定的泛型为单位
 * result：当后台任务执行完毕时，需要对结果进行返回，这里指定什么泛型就返回什么样的数据类型，比如，这里指定boolean，则后台任务执行完毕返回true或者false
 */
public class DownloadTask extends AsyncTask<String,Integer,Integer> {


    private static final String TAG = "DownloadTask";
    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

    private DownloadListener listener;

    private Boolean isCancled = false;

    private Boolean isPaused = false;

    private int lastProgress;


    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... params) {

        InputStream is = null;
        RandomAccessFile savedFile = null ;
        File file = null;
        try {
            long downloadFileLength = 0; //记录已下载的文件长度
            String downloadUrl = params[0];
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            //--->  /storage/emulated/0/Download
            String directroy = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            Log.i(TAG, "directroyPath--->" + directroy);
            file = new File(directroy+fileName);
            if(file.exists()){  //存在，拿到这个文件已经下载的长度，从这里开始下载
                downloadFileLength = file.length();
            }
            //获得这个下载文件的总长度,使用okhttp
            long contentLength = getContentLength(downloadUrl);
            if(contentLength==0){//url地址文件长度为0，下载失败
                return TYPE_FAILED;
            }else if(contentLength==downloadFileLength){ //下载完成
                return TYPE_SUCCESS;
            }
            //运行到这里，说明既不会这个url地址有问题，也不会说这个已经下载的文件长度已经下载完成了
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                                         .url(downloadUrl)
                                         .addHeader("RANGE","bytes="+downloadFileLength+"-")
                                         .build();
            Response response = client.newCall(request).execute();//同步堵塞
            if(response!=null){
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file, "rw");
                savedFile.seek(downloadFileLength);
                byte[] b = new byte[1024];
                int total = 0;
                int len ;
                while((len=is.read(b)) != -1){
                    if(isCancled){
                        return TYPE_CANCELED;
                    }else if(isPaused){
                        return TYPE_PAUSED;
                    }else{
                        total+=len;
                        savedFile.write(b, 0, len);

                        //计算已经下载的百分比

                        int progress = (int) ((total + downloadFileLength) * 100 / contentLength);
                        publishProgress(progress);

                    }
                }
                //当运行到这里说明将url文件剩下的长度读写到文件中了
                response.body().close();
                return TYPE_SUCCESS;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (savedFile != null) {
                    savedFile.close();
                }
                    if (isCancled && file != null) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    /**
     * 获取目标文件的长度
     * @param url
     * @return
     */
    private long getContentLength(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                                     .url(url)
                                     .build();
        Response response =  client.newCall(request).execute();
        if(response!=null && response.isSuccessful()){
            long contentLength =  response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }


    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if(progress>lastProgress){
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer integer) {

        switch (integer){

            case TYPE_CANCELED:
                listener.onCanccled();
                break;

            case TYPE_FAILED:
                listener.onFailed();
                break;

            case TYPE_PAUSED:
                listener.onPaused();
                break;

            case TYPE_SUCCESS:
                listener.onSuccess();
                break;

        }
    }

    public void pausedDownload(){

       if(!isPaused){
           isPaused=true;
       }else{
           isPaused = false;
       }
//        isPaused = true;
    }

    public void cancelDownload(){
        isCancled = true;
    }




}
