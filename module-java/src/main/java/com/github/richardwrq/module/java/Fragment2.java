package com.github.richardwrq.module.java;

import android.app.Fragment;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.richardwrq.common.RouterTable;
import com.github.richardwrq.krouter.annotation.Inject;
import com.github.richardwrq.krouter.annotation.Route;
import com.github.richardwrq.krouter.api.core.KRouter;

import org.rc.wrq.module.java.R;

/**
 * @author WuRuiQiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/2/28 上午9:14
 */
@Route(path = RouterTable.FGM2_PATH)
public class Fragment2 extends Fragment {

    @Inject
    String test;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_2, container, false);
        KRouter.INSTANCE.inject(this);
        ((TextView) view.findViewById(R.id.tv)).setText(test);
        return view;
    }
}
