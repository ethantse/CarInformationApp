package com.explore

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.car.R
import kotlinx.android.synthetic.main.activity_explore.*
import kotlinx.android.synthetic.main.layout_card.*
import ethan.demo.library.CardLayoutHelper
import ethan.demo.library.OnCardLayoutListener
import com.explore.ToolBarEntity
import java.util.*


class MainActivity : AppCompatActivity() {


    private var list = ArrayList<CardEntity>()
    private var cardAdapter : CardAdapter? = null

    private lateinit var mCardLayoutHelper : CardLayoutHelper<CardEntity>


    private lateinit var mConfig : CardLayoutHelper.Config

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explore)
        setStatusBar()

        mCardLayoutHelper = CardLayoutHelper()

        mConfig = CardLayoutHelper.Config()
                .setCardCount(2)
                .setMaxRotation(20f)
                .setOffset(8.dp)
                .setSwipeThreshold(0.2f)
                .setDuration(200)

        mCardLayoutHelper.setConfig(mConfig)

        mCardLayoutHelper.attachToRecyclerView(recycler_view5)

        mCardLayoutHelper.bindDataSource(object : CardLayoutHelper.BindDataSource<CardEntity> {
            override fun bind(): List<CardEntity> {
                return list
            }
        })

        mCardLayoutHelper.setOnCardLayoutListener(object : OnCardLayoutListener {
            override fun onSwipe(dx: Float, dy: Float) {
                Log.d("onStateChanged","dx:$dx dy:$dy")
            }

            override fun onStateChanged(state: CardLayoutHelper.State) {
                Log.d("onStateChanged",state.name)

            }

        })

        cardAdapter = CardAdapter(list)

        recycler_view5.adapter =cardAdapter

        btn_prev.setOnClickListener {
            if (mCardLayoutHelper.canBack()){
                mCardLayoutHelper.doBack()
            }
        }

        btn_next.setOnClickListener {
            onNextPressed()
        }

        btn_menu.setOnClickListener {
            pull_down_layout.openMenu()
        }

        getMockData()

        //设置阻尼
        pull_down_layout.setDragRatio(0.6f)
        //设置视觉差系数
        pull_down_layout.setParallaxRatio(1.1f)
        //设置动画时长
        pull_down_layout.setDuration(200)

        updateConfigShow()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> castList(input: List<*>):List<T>{
        return input as List<T>
    }

    private fun getMockData(){
        MockData.getCards(this) {
            val cards = it["cards"]
            if (cards is List<*>) {
                list.clear()
                list.addAll(castList(cards))
                cardAdapter?.notifyDataSetChanged()
            }
            val toolbarItems = it["toolbarItems"]
            if (toolbarItems is List<*>) {
                val items = castList<ToolBarEntity>(toolbarItems)
                if (!items.isEmpty()) {
                    layout_top_menu.removeAllViews()
                    for (item in items) {
                        val linearLayout = LinearLayout(this)
                        linearLayout.gravity = Gravity.CENTER
                        linearLayout.orientation = LinearLayout.VERTICAL
                        val iv = ImageView(this)
                        val layoutParams1 = LinearLayout.LayoutParams(66.dp, 66.dp)
                        layoutParams1.bottomMargin = 10.dp
                        linearLayout.addView(iv, layoutParams1)
                        Glide.with(this).load(item.picUrl).apply(RequestOptions.circleCropTransform()).into(iv)

                        val tv = TextView(this)
                        tv.textSize = 12f
                        tv.setTextColor(Color.parseColor("#333333"))
                        tv.text = item.title
                        tv.gravity = Gravity.CENTER
                        linearLayout.addView(tv)

                        val layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
                        layoutParams.weight = 1f
                        layout_top_menu.addView(linearLayout, layoutParams)
                        linearLayout.setOnClickListener {
                            Toast.makeText(this, item.title, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun setStatusBar(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = Color.TRANSPARENT
            pull_down_layout.setPadding(pull_down_layout.paddingLeft,getStatusBarHeight(this),pull_down_layout.paddingRight,pull_down_layout.paddingBottom)
        }
    }

    private fun getStatusBarHeight(context: Context): Int {
        var statusBarHeight = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

    private fun onNextPressed(){
        if (mCardLayoutHelper.canNext()) {
            mCardLayoutHelper.doNext()
        }
    }

    override fun onBackPressed() {
        if (mCardLayoutHelper.canBack()){
            mCardLayoutHelper.doBack()
        }else if (mCardLayoutHelper.noBack()){
            super.onBackPressed()
        }
    }

    private fun updateConfigShow(){
        val text = "卡片数:${mConfig.cardCount} | 偏移像素:${mConfig.offset} | 最大旋转角度:${mConfig.maxRotation} | 拖拽触发阈值:${mConfig.swipeThreshold} | 下拉菜单阻尼:${pull_down_layout.getDragRatio()} | 下拉视觉差比例:${pull_down_layout.getParallaxRatio()}"
        val list = text.split("|")
        val spannableString = SpannableString(text)
        var start = 0
        for (index in 0 until list.size){
            val item = list[index]
            val end = start + item.length
            spannableString.setSpan(object :ClickableSpan(){
                override fun onClick(widget: View) {
                    when(index){
                        0->{
                            showDialog(index,item,1,mConfig.cardCount,3,1)
                        }
                        1->{
                            showDialog(index,item,0,mConfig.offset,20.dp,1)
                        }
                        2->{
                            val divisor = 2
                            showDialog(index,item,0,mConfig.maxRotation.toInt()*divisor,120,divisor)
                        }
                        3->{
                            val divisor = 10
                            showDialog(index,item,1, (mConfig.swipeThreshold*divisor).toInt(),8,divisor)
                        }
                        4->{
                            val divisor = 10
                            showDialog(index,item,2, (pull_down_layout.getDragRatio()*divisor).toInt(),12,divisor)
                        }
                        5->{
                            val divisor = 10
                            showDialog(index,item,8,(pull_down_layout.getParallaxRatio()*divisor).toInt(),15,divisor)
                        }

                    }
                }
            },start,end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            start+=item.length + 1
        }
        tv_show_config.text = spannableString
        tv_show_config.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun showDialog(index:Int,title: String,start:Int,cur:Int,end:Int,divisor:Int){
        val settingDialogFragment = SettingDialogFragment.newInstance(title,start,cur,end,divisor)
        settingDialogFragment.show(supportFragmentManager,"dialog_setting")
        settingDialogFragment.setCallback {
            when(index){
                0->{
                    mConfig.cardCount = it.toInt()
                    recycler_view5.adapter?.notifyDataSetChanged()
                }
                1->{
                    mConfig.offset = it.toInt()
                    recycler_view5.adapter?.notifyDataSetChanged()
                }
                2->{
                    mConfig.maxRotation = it.toFloat()
                    recycler_view5.adapter?.notifyDataSetChanged()
                }
                3->{
                    mConfig.swipeThreshold = it.toFloat()
                    recycler_view5.adapter?.notifyDataSetChanged()
                }
                4->{
                    pull_down_layout.setDragRatio(it.toFloat())
                }
                5->{
                    pull_down_layout.setParallaxRatio(it.toFloat())
                }

                else -> {

                }
            }
            updateConfigShow()
        }
    }

}
