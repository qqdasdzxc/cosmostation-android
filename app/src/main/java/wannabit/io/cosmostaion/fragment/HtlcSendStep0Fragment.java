package wannabit.io.cosmostaion.fragment;

import static wannabit.io.cosmostaion.base.BaseConstant.BINANCE_TOKEN_IMG_URL;
import static wannabit.io.cosmostaion.base.BaseConstant.KAVA_COIN_IMG_URL;
import static wannabit.io.cosmostaion.base.BaseConstant.TOKEN_HTLC_BINANCE_BNB;
import static wannabit.io.cosmostaion.base.BaseConstant.TOKEN_HTLC_BINANCE_BTCB;
import static wannabit.io.cosmostaion.base.BaseConstant.TOKEN_HTLC_BINANCE_BUSD;
import static wannabit.io.cosmostaion.base.BaseConstant.TOKEN_HTLC_BINANCE_XRPB;
import static wannabit.io.cosmostaion.base.BaseConstant.TOKEN_HTLC_KAVA_BNB;
import static wannabit.io.cosmostaion.base.BaseConstant.TOKEN_HTLC_KAVA_BTCB;
import static wannabit.io.cosmostaion.base.BaseConstant.TOKEN_HTLC_KAVA_BUSD;
import static wannabit.io.cosmostaion.base.BaseConstant.TOKEN_HTLC_KAVA_XRPB;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.squareup.picasso.Picasso;

import java.math.BigDecimal;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import wannabit.io.cosmostaion.R;
import wannabit.io.cosmostaion.activities.HtlcSendActivity;
import wannabit.io.cosmostaion.base.BaseChain;
import wannabit.io.cosmostaion.base.BaseFragment;
import wannabit.io.cosmostaion.dialog.Dialog_Htlc_Receive_Chain;
import wannabit.io.cosmostaion.dialog.Dialog_Htlc_Send_Coin;
import wannabit.io.cosmostaion.network.ApiClient;
import wannabit.io.cosmostaion.network.res.ResKavaBep3Param;
import wannabit.io.cosmostaion.network.res.ResKavaSwapSupply;
import wannabit.io.cosmostaion.utils.WDp;

public class HtlcSendStep0Fragment extends BaseFragment implements View.OnClickListener {
    public final static int SELECT_DESTINATION_CHAIN = 9100;
    public final static int SELECT_TO_SEND_COIN = 9101;

    private Button mBtnCancel, mBtnNext;

    private ImageView mFromChainImg;
    private TextView mFromChainTv;

    private RelativeLayout mBtnToChain;
    private ImageView mToChainImg;
    private TextView mToChainTv;

    private RelativeLayout mBtnToSendCoin;
    private ImageView mToSendCoinImg;
    private TextView mToSendCoinTv, mToSendCoindenom, mToSendCoinAvailable;

    private LinearLayout mCapLayer;
    private TextView mOnceMaxAmount, mOnceMaxDenom;
    private TextView mSystemMaxAmount, mSystemMaxDenom;
    private TextView mRemainAmount, mRemainDenom;

    private ArrayList<BaseChain> mToChainList;
    private BaseChain mToChain;
    private ArrayList<String> mSwappableCoinList;
    private String mToSwapDenom;

    private ResKavaBep3Param mKavaBep3Param2;
    private ResKavaSwapSupply mKavaSuppies2;

    private BigDecimal supply_limit = BigDecimal.ZERO;
    private BigDecimal supply_remain = BigDecimal.ZERO;
    private BigDecimal onetime_max = BigDecimal.ZERO;
    private BigDecimal available_amount = BigDecimal.ZERO;

    public static HtlcSendStep0Fragment newInstance(Bundle bundle) {
        HtlcSendStep0Fragment fragment = new HtlcSendStep0Fragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_htlc_send_step0, container, false);
        mBtnCancel = rootView.findViewById(R.id.btn_cancel);
        mBtnNext = rootView.findViewById(R.id.btn_next);
        mFromChainImg = rootView.findViewById(R.id.img_from_chain);
        mFromChainTv = rootView.findViewById(R.id.txt_from_chain);
        mBtnToChain = rootView.findViewById(R.id.btn_to_chain);
        mToChainImg = rootView.findViewById(R.id.img_to_chain);
        mToChainTv = rootView.findViewById(R.id.txt_to_chain);
        mBtnToSendCoin = rootView.findViewById(R.id.btn_to_send_coin);
        mToSendCoinImg = rootView.findViewById(R.id.img_to_send_coin);
        mToSendCoinTv = rootView.findViewById(R.id.txt_to_send_coin);
        mToSendCoindenom = rootView.findViewById(R.id.txt_to_send_denom);
        mToSendCoinAvailable = rootView.findViewById(R.id.txt_to_send_available);
        mCapLayer = rootView.findViewById(R.id.cap_layer);
        mOnceMaxAmount = rootView.findViewById(R.id.once_max);
        mOnceMaxDenom = rootView.findViewById(R.id.once_max_denom);
        mSystemMaxAmount = rootView.findViewById(R.id.system_max_amount);
        mSystemMaxDenom = rootView.findViewById(R.id.system_max_denom);
        mRemainAmount = rootView.findViewById(R.id.remain_cap_amount);
        mRemainDenom = rootView.findViewById(R.id.remain_cap_denom);

        mBtnCancel.setOnClickListener(this);
        mBtnNext.setOnClickListener(this);
        mBtnToChain.setOnClickListener(this);
        mBtnToSendCoin.setOnClickListener(this);

        mToChainList = BaseChain.getHtlcSendable(getSActivity().mBaseChain);
        if (mToChainList.size() <= 0) {
            getSActivity().onBeforeStep();
        }
        mToChain = mToChainList.get(0);

        mSwappableCoinList = BaseChain.getHtlcSwappableCoin(getSActivity().mBaseChain);
        if (mSwappableCoinList.size() <= 0) {
            getSActivity().onBeforeStep();
        }
        mToSwapDenom = getSActivity().mToSwapDenom;

        onCheckSwapParam();
        onUpdateView();
        return rootView;
    }

    private void onUpdateView() {
        if (mToChain == null) {
            getSActivity().onBeforeStep();
        }
        WDp.onDpSwapChain(getContext(), getSActivity().mBaseChain, mFromChainImg, mFromChainTv);
        WDp.onDpSwapChain(getContext(), mToChain, mToChainImg, mToChainTv);
        mToSendCoindenom.setText("(" + mToSwapDenom + ")");

        if (getSActivity().mBaseChain.equals(BaseChain.BNB_MAIN) && (mKavaBep3Param2 != null && mKavaSuppies2 != null)) {
            mCapLayer.setVisibility(View.VISIBLE);
            if (mToSwapDenom.equals(TOKEN_HTLC_BINANCE_BNB)) {
                mToSendCoinImg.setImageDrawable(ContextCompat.getDrawable(getSActivity(), R.drawable.bnb_token_img));
                onSetDpDenom(getString(R.string.str_bnb_c));
            } else if (mToSwapDenom.equals(TOKEN_HTLC_BINANCE_BTCB)) {
                onSetDpDenom("BTC");
                try {
                    Picasso.get().load(BINANCE_TOKEN_IMG_URL + "BTCB.png").into(mToSendCoinImg);
                } catch (Exception e) {
                }

            } else if (mToSwapDenom.equals(TOKEN_HTLC_BINANCE_XRPB)) {
                onSetDpDenom("XRP");
                try {
                    Picasso.get().load(BINANCE_TOKEN_IMG_URL + "XRP.png").into(mToSendCoinImg);
                } catch (Exception e) {
                }

            } else if (mToSwapDenom.equals(TOKEN_HTLC_BINANCE_BUSD)) {
                onSetDpDenom("BUSD");
                try {
                    Picasso.get().load(BINANCE_TOKEN_IMG_URL + "BUSD.png").into(mToSendCoinImg);
                } catch (Exception e) {
                }

            }
            available_amount = getSActivity().mAccount.getTokenBalance(mToSwapDenom);
            supply_limit = mKavaBep3Param2.getSupportedSwapAssetLimit(mToSwapDenom);
            supply_remain = mKavaSuppies2.getRemainCap(mToSwapDenom, supply_limit);
            onetime_max = mKavaBep3Param2.getSupportedSwapAssetMaxOnce(mToSwapDenom);
            mToSendCoinAvailable.setText(WDp.getDpAmount2(getContext(), available_amount, 0, 8));

        } else if (getSActivity().mBaseChain.equals(BaseChain.KAVA_MAIN) && (mKavaBep3Param2 != null && mKavaSuppies2 != null)) {
            mCapLayer.setVisibility(View.GONE);
            if (mToSwapDenom.equals(TOKEN_HTLC_KAVA_BNB)) {
                mToSendCoinImg.setImageDrawable(ContextCompat.getDrawable(getSActivity(), R.drawable.bnb_on_kava));
                onSetDpDenom(getString(R.string.str_bnb_c));
            } else if (mToSwapDenom.equals(TOKEN_HTLC_KAVA_BTCB)) {
                onSetDpDenom("BTC");
                try {
                    Picasso.get().load(KAVA_COIN_IMG_URL + "btcb.png").into(mToSendCoinImg);
                } catch (Exception e) {
                }

            } else if (mToSwapDenom.equals(TOKEN_HTLC_KAVA_XRPB)) {
                onSetDpDenom("XRP");
                try {
                    Picasso.get().load(KAVA_COIN_IMG_URL + "xrpb.png").into(mToSendCoinImg);
                } catch (Exception e) {
                }

            } else if (mToSwapDenom.equals(TOKEN_HTLC_KAVA_BUSD)) {
                onSetDpDenom("BUSD");
                try {
                    Picasso.get().load(KAVA_COIN_IMG_URL + "busd.png").into(mToSendCoinImg);
                } catch (Exception e) {
                }

            }
            available_amount = getBaseDao().getAvailable(mToSwapDenom);
            supply_limit = mKavaBep3Param2.getSupportedSwapAssetLimit(mToSwapDenom);
            supply_remain = mKavaSuppies2.getRemainCap(mToSwapDenom, supply_limit);
            onetime_max = mKavaBep3Param2.getSupportedSwapAssetMaxOnce(mToSwapDenom);
            mToSendCoinAvailable.setText(WDp.getDpAmount2(getContext(), available_amount, 8, 8));

        }

        mOnceMaxAmount.setText(WDp.getDpAmount2(getContext(), onetime_max, 8, 8));
        mSystemMaxAmount.setText(WDp.getDpAmount2(getContext(), supply_limit, 8, 8));
        mRemainAmount.setText(WDp.getDpAmount2(getContext(), supply_remain, 8, 8));


    }

    private void onSetDpDenom(String dpDenom) {
        mToSendCoinTv.setText(dpDenom);
        mOnceMaxDenom.setText(dpDenom);
        mSystemMaxDenom.setText(dpDenom);
        mRemainDenom.setText(dpDenom);

    }

    @Override
    public void onClick(View v) {
        if (v.equals(mBtnToChain)) {
            Bundle bundle = new Bundle();
            bundle.putString("chainName", getSActivity().mBaseChain.getChain());
            Dialog_Htlc_Receive_Chain dialog = Dialog_Htlc_Receive_Chain.newInstance(bundle);
            dialog.setCancelable(true);
            dialog.setTargetFragment(this, SELECT_DESTINATION_CHAIN);
            getFragmentManager().beginTransaction().add(dialog, "dialog").commitNowAllowingStateLoss();

        } else if (v.equals(mBtnToSendCoin)) {
            Bundle bundle = new Bundle();
            bundle.putString("chainName", getSActivity().mBaseChain.getChain());
            Dialog_Htlc_Send_Coin dialog = Dialog_Htlc_Send_Coin.newInstance(bundle);
            dialog.setCancelable(true);
            dialog.setTargetFragment(this, SELECT_TO_SEND_COIN);
            getFragmentManager().beginTransaction().add(dialog, "dialog").commitNowAllowingStateLoss();

        } else if (v.equals(mBtnCancel)) {
            getSActivity().onBeforeStep();

        } else if (v.equals(mBtnNext)) {
            if (supply_remain.compareTo(BigDecimal.ZERO) <= 0) {
                Toast.makeText(getContext(), R.string.error_bep3_supply_full, Toast.LENGTH_SHORT).show();

            } else if (!onCheckMinBalance()) {
                Toast.makeText(getContext(), R.string.error_bep3_under_min_amount, Toast.LENGTH_SHORT).show();

            } else {
                getSActivity().mRecipientChain = mToChain;
                getSActivity().mToSwapDenom = mToSwapDenom;
                getSActivity().mTotalCap = supply_limit;
                getSActivity().mRemainCap = supply_remain;
                getSActivity().mMaxOnce = onetime_max;
                getSActivity().onNextStep();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_DESTINATION_CHAIN && resultCode == Activity.RESULT_OK) {
            mToChain = mToChainList.get(data.getIntExtra("position", 0));
            onUpdateView();

        } else if (requestCode == SELECT_TO_SEND_COIN && resultCode == Activity.RESULT_OK) {
            mToSwapDenom = mSwappableCoinList.get(data.getIntExtra("position", 0));
            onUpdateView();
        }
    }

    private void onCheckSwapParam() {
        if (getSActivity().mBaseChain.equals(BaseChain.BNB_MAIN) || getSActivity().mBaseChain.equals(BaseChain.KAVA_MAIN)) {
            ApiClient.getKavaChain(getContext()).getSwapParams2().enqueue(new Callback<ResKavaBep3Param>() {
                @Override
                public void onResponse(Call<ResKavaBep3Param> call, Response<ResKavaBep3Param> response) {
                    if (!response.isSuccessful()) {
                        Toast.makeText(getContext(), R.string.error_network_error, Toast.LENGTH_SHORT).show();
                    } else {
                        mKavaBep3Param2 = response.body();
                        getSActivity().mKavaBep3Param2 = mKavaBep3Param2;
                        onCheckSwapSupply();
                    }
                }

                @Override
                public void onFailure(Call<ResKavaBep3Param> call, Throwable t) {
                    Toast.makeText(getContext(), R.string.error_network_error, Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    private void onCheckSwapSupply() {
        if (getSActivity().mBaseChain.equals(BaseChain.BNB_MAIN) || getSActivity().mBaseChain.equals(BaseChain.KAVA_MAIN)) {
            ApiClient.getKavaChain(getContext()).getSupplies2().enqueue(new Callback<ResKavaSwapSupply>() {
                @Override
                public void onResponse(Call<ResKavaSwapSupply> call, Response<ResKavaSwapSupply> response) {
                    if (!response.isSuccessful()) {
                        Toast.makeText(getContext(), R.string.error_network_error, Toast.LENGTH_SHORT).show();
                    } else {
                        mKavaSuppies2 = response.body();
                        getSActivity().mKavaSuppies2 = mKavaSuppies2;
                        onUpdateView();
                    }
                }

                @Override
                public void onFailure(Call<ResKavaSwapSupply> call, Throwable t) {
                    Toast.makeText(getContext(), R.string.error_network_error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean onCheckMinBalance() {
        if (getSActivity().mBaseChain.equals(BaseChain.BNB_MAIN)) {
            if (available_amount.compareTo(mKavaBep3Param2.getSupportedSwapAssetMin(mToSwapDenom).movePointLeft(8)) > 0) {
                return true;
            }

        } else if (getSActivity().mBaseChain.equals(BaseChain.KAVA_MAIN)) {
            if (available_amount.compareTo(mKavaBep3Param2.getSupportedSwapAssetMin(mToSwapDenom)) > 0) {
                return true;
            }
        }
        return false;
    }

    private HtlcSendActivity getSActivity() {
        return (HtlcSendActivity) getBaseActivity();
    }

}
