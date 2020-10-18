package jp.techacademy.youichi.nozaka.taskapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import io.realm.RealmChangeListener
import io.realm.Sort
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.app.AlarmManager
import android.app.PendingIntent

const val EXTRA_TASK = "jp.techacademy.youichi.nozaka.taskapp.TASK"

class MainActivity : AppCompatActivity() {
    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }

    // カテゴリ検索用の変数を定義
    var cat_search = ""

    private lateinit var mTaskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        fab.setOnClickListener { view ->
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            startActivity(intent)
        }

        search_button.setOnClickListener {
            if(search_edit_text.text.toString() != "") {
                cat_search = search_edit_text.text.toString()
                reloadCategoryListView()
            }
            else
                reloadListView()
        }

        reset_button.setOnClickListener {
            reloadListView()
        }

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this@MainActivity)

        // ListViewをタップしたときの処理
        listView1.setOnItemClickListener { parent, _, position, _ ->
            // 入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, _, position, _ ->
            // タスクを削除する
            val task = parent.adapter.getItem(position) as Task

            // ダイアログを表示する
            val builder = AlertDialog.Builder(this@MainActivity)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK"){_, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this@MainActivity,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                // タスク削除後のListView更新
                if(cat_search != "")
                    reloadCategoryListView() // カテゴリ検索用ListViewを表示
                else
                    reloadListView()
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }

        reloadListView()
        // reloadCategoryListView()
    }

    private fun reloadListView() {
        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        val taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)

        // 上記の結果を、TaskList としてセットする
        mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    private fun reloadCategoryListView(){
        // Realmデータベースから、「全てのデータを取得し、登録されたカテゴリと検索語が一致するものを抽出し、新しい日時順に並べた結果」を取得
        val taskRealmResults = mRealm.where(Task::class.java).equalTo("category", cat_search).findAll().sort("date",Sort.DESCENDING)

        // 上記の結果を、TaskList としてセットする
        mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }
}