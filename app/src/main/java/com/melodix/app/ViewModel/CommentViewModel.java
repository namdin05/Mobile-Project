package com.melodix.app.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Model.Comment;
import com.melodix.app.Repository.CommentRepository;

import java.util.List;

public class CommentViewModel extends AndroidViewModel {
    private CommentRepository repository;

    private MutableLiveData<Boolean> actionSuccess = new MutableLiveData<>();
    private MutableLiveData<String> actionMessage = new MutableLiveData<>();

    private MutableLiveData<List<Comment>> commentsList = new MutableLiveData<>();
    private MutableLiveData<String> fetchMessage = new MutableLiveData<>();

    public LiveData<List<Comment>> getCommentsList() { return commentsList; }
    public LiveData<String> getFetchMessage() { return fetchMessage; }

    // Hàm ra lệnh tải bình luận
    public void fetchComments(String songId) {
        repository.getCommentsBySong(songId, commentsList, fetchMessage);
    }

    public CommentViewModel(@NonNull Application application) {
        super(application);
        repository = new CommentRepository(application);
    }

    public LiveData<Boolean> getActionSuccess() { return actionSuccess; }
    public LiveData<String> getActionMessage() { return actionMessage; }

    // Fragment chỉ cần gọi hàm này
    public void postNewComment(String songId, String content) {
        repository.postComment(songId, content, actionSuccess, actionMessage);
    }
}
