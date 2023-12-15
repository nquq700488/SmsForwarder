package com.idormy.sms.forwarder.fragment.condition

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentTasksConditionToAddressBinding
import com.idormy.sms.forwarder.entity.task.LocationSetting
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_CONDITION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_CONDITION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_CONDITION
import com.idormy.sms.forwarder.utils.KEY_TEST_CONDITION
import com.idormy.sms.forwarder.utils.TASK_CONDITION_TO_ADDRESS
import com.idormy.sms.forwarder.utils.XToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar

@Page(name = "ToAddress")
@Suppress("PrivatePropertyName")
class ToAddressFragment : BaseFragment<FragmentTasksConditionToAddressBinding?>(), View.OnClickListener {

    private val TAG: String = ToAddressFragment::class.java.simpleName
    var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_CONDITION)
    var eventData: String? = null

    private var description = ""

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksConditionToAddressBinding {
        return FragmentTasksConditionToAddressBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_to_address)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        //测试按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnTest, 3)
        mCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnTest.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnTest.text = getString(R.string.test)
            }
        })

        binding!!.rgCalcType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_calc_type_distance) {
                binding!!.layoutCalcTypeDistance.visibility = View.VISIBLE
                binding!!.layoutCalcTypeAddress.visibility = View.GONE
            } else {
                binding!!.layoutCalcTypeDistance.visibility = View.GONE
                binding!!.layoutCalcTypeAddress.visibility = View.VISIBLE
            }
        }

        Log.d(TAG, "initViews eventData:$eventData")
        if (eventData != null) {
            val settingVo = Gson().fromJson(eventData, LocationSetting::class.java)
            Log.d(TAG, "initViews settingVo:$settingVo")
            binding!!.rgCalcType.check(settingVo.getCalcTypeCheckId())
            binding!!.etLongitude.setText(settingVo.longitude.toString())
            binding!!.etLatitude.setText(settingVo.latitude.toString())
            binding!!.etDistance.setText(settingVo.distance.toString())
            binding!!.etAddress.setText(settingVo.address)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun initListeners() {
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        LiveEventBus.get(KEY_TEST_CONDITION, String::class.java).observe(this) {
            mCountDownHelper?.finish()

            if (it == "success") {
                XToastUtils.success("测试通过", 30000)
            } else {
                XToastUtils.error(it, 30000)
            }
        }
    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.btn_test -> {
                    mCountDownHelper?.start()
                    Thread {
                        try {
                            val settingVo = checkSetting()
                            Log.d(TAG, settingVo.toString())
                            LiveEventBus.get(KEY_TEST_CONDITION, String::class.java).post("success")
                        } catch (e: Exception) {
                            LiveEventBus.get(KEY_TEST_CONDITION, String::class.java).post(e.message.toString())
                            e.printStackTrace()
                        }
                    }.start()
                    return
                }

                R.id.btn_del -> {
                    popToBack()
                    return
                }

                R.id.btn_save -> {
                    val settingVo = checkSetting()
                    val intent = Intent()
                    intent.putExtra(KEY_BACK_DESCRIPTION_CONDITION, description)
                    intent.putExtra(KEY_BACK_DATA_CONDITION, Gson().toJson(settingVo))
                    setFragmentResult(TASK_CONDITION_TO_ADDRESS, intent)
                    popToBack()
                    return
                }
            }
        } catch (e: Exception) {
            XToastUtils.error(e.message.toString(), 30000)
            e.printStackTrace()
        }
    }

    //检查设置
    @SuppressLint("SetTextI18n")
    private fun checkSetting(): LocationSetting {
        val longitude = binding!!.etLongitude.text.toString().toDouble()
        val latitude = binding!!.etLatitude.text.toString().toDouble()
        val distance = binding!!.etDistance.text.toString().toDouble()
        val address = binding!!.etAddress.text.toString()
        var calcType = "distance"
        if (binding!!.rbCalcTypeDistance.isChecked) {
            if (latitude.isNaN() || longitude.isNaN() || distance.isNaN()) {
                throw Exception(getString(R.string.calc_type_address_error))
            }
            description = String.format(getString(R.string.to_address_distance_description), longitude, latitude, distance)
        } else if (binding!!.rbCalcTypeAddress.isChecked) {
            if (address.isEmpty()) {
                throw Exception(getString(R.string.calc_type_address_error))
            }
            description = String.format(getString(R.string.to_address_keyword_description), address)
            calcType = "address"
        }

        return LocationSetting(description, "to", calcType, longitude, latitude, distance, address)
    }
}