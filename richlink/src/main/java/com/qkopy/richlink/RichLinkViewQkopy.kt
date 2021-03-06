package com.qkopy.richlink

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import com.bumptech.glide.Glide
import com.qkopy.richlink.data.database.MetaDatabase
import com.qkopy.richlink.data.model.MetaData
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread


open class RichLinkViewQkopy : RelativeLayout {


    private var view: View?=null
    var metaData: MetaData? = null
    internal var context: Context


    private lateinit var linearLayout: LinearLayout
    private lateinit var imageView: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView

    private var mainUrl: String? = null

    private var isDefaultClick = false

    private var richLinkListener: RichLinkListener? = null

    constructor(context: Context) : super(context) {
        this.context = context
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.context = context
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        this.context = context
    }

    fun initView() {



        if (findLinearLayoutChild() != null) {
            this.view = findLinearLayoutChild()
        } else {
            this.view = this
            View.inflate(context, R.layout.qkopy_link_layout, this)
        }


        linearLayout = findViewById<View>(R.id.linearLayout) as LinearLayout
        imageView = findViewById<View>(R.id.imageViewBanner) as ImageView
        tvTitle = findViewById<View>(R.id.textViewTitle) as TextView
        tvDescription = findViewById<View>(R.id.textViewDescription) as TextView




        imageView.visibility = View.VISIBLE
        Glide.with(context).load(metaData?.image).placeholder(R.drawable.notfound).error(R.drawable.notfound).into(imageView)
        tvTitle.text = metaData?.title

        if (metaData?.description?.isEmpty() == true || metaData?.description == "") {
            tvDescription.visibility = View.GONE
        } else {
            tvDescription.visibility = View.VISIBLE
            tvDescription.text = metaData?.description
        }
        tvDescription.text = metaData?.url ?: mainUrl


        linearLayout.setOnClickListener { view ->
            if (isDefaultClick) {
                richLinkClicked()
            } else {
                if (richLinkListener != null) {
                    richLinkListener?.onClicked(view, metaData)
                } else {
                    richLinkClicked()
                }
            }
        }

    }

    private fun richLinkClicked() {
//                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mainUrl))
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        context.startActivity(intent)
//        // CustomTabsIntent.Builder used to configure CustomTabsIntent.
//        val builder = CustomTabsIntent.Builder().setShowTitle(true)
//        builder.setToolbarColor(ResourcesCompat.getColor(resources, R.color.blue_grey_300, null))
//        // For adding menu item
//        builder.addMenuItem("Share", getItem())
//        builder.addDefaultShareMenuItem()
//        // CustomTabsIntent used to launch the URL
//        val customTabsIntent = builder.build()
//        // Open the Custom Tab
//        customTabsIntent.launchUrl(context, Uri.parse(mainUrl))

     val builder = CustomTabsIntent.Builder()
     val customTabsIntent = builder.build()
     customTabsIntent.launchUrl(context, Uri.parse(mainUrl))

    }

    private fun getItem(): PendingIntent {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, mainUrl)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, mainUrl)
        println("main url $mainUrl")
        return PendingIntent.getActivity(context, 0, shareIntent, 0)
    }

    private fun findLinearLayoutChild(): LinearLayout? {
        return if (childCount > 0 && getChildAt(0) is LinearLayout) {
            getChildAt(0) as LinearLayout
        } else null
    }

    fun setLinkFromMeta(metaData: MetaData) {
        this.metaData = metaData
//        initView()
    }

    fun setDefaultClickListener(isDefault: Boolean) {
        isDefaultClick = isDefault
    }

    fun setClickListener(richLinkListener1: RichLinkListener) {
        richLinkListener = richLinkListener1
    }

    fun setLink(url: String, context: Context, viewListener: ViewListener) {
        this.context = context
        mainUrl = url
        if (!mainUrl.isNullOrEmpty()) {
            val metaDataBase = MetaDatabase.getInstance(context)
            doAsync {
                val meta = metaDataBase.metaDataDao().getMetaDataUrl(url)
                uiThread {
                    if (meta != null) {
                        metaData = meta
                        viewListener.onSuccess(true)
                        initView()
                    } else {
                        val richPreview = RichPreview(object : ResponseListener {
                            override fun onData(meta: MetaData?) {
                                metaData = meta
                                if (metaData?.title?.isEmpty() == false || metaData?.title == "") {
                                    viewListener.onSuccess(true)
                                    doAsync {
                                        metaDataBase.metaDataDao().insert(metaData!!)
                                        metaDataBase.metaDataDao().delete()
                                    }
                                    initView()
                                }
                            }

                            override fun onError(e: Exception) {
                                initView()
                                viewListener.onError(e)
                            }
                        })

                        richPreview.getPreview(url)
                    }
                }
            }

        }

    }


}
