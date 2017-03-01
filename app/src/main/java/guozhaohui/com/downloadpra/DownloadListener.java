package guozhaohui.com.downloadpra;

/**
 * Created by ${GuoZhaoHui} on 2017/2/27.
 * email:guozhaohui628@gmail.com
 */

public interface DownloadListener {

    void onProgress(int progress);
    void onSuccess();
    void onFailed();
    void onPaused();
    void onCanccled();

}
