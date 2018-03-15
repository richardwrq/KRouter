package com.github.richardwrq.krouter.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.richardwrq.krouter.R;
import com.github.richardwrq.common.RouterTable;
import com.github.richardwrq.krouter.annotation.Inject;
import com.github.richardwrq.krouter.annotation.Route;
import com.github.richardwrq.krouter.api.core.KRouter;

/**
 * @author WuRuiQiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/2/28 上午9:13
 */
@Route(path = RouterTable.FGM1_PATH)
public class Fragment1 extends Fragment {

    @Inject(name = "test")
    String content;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_1, container, false);
        KRouter.INSTANCE.inject(this);
        ((TextView) view.findViewById(R.id.tv)).setText(content);
        view.findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                KRouter.INSTANCE.create(RouterTable.RESULT_ATY_PATH)
                        .withRequestCode(Fragment1.this, 2)
                        .request();
            }
        });
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(getActivity(), "result : " + data.getStringExtra("result"), Toast.LENGTH_SHORT).show();
    }
}
