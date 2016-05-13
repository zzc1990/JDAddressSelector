package chihane.jdaddressselector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.raizlabs.android.dbflow.list.FlowQueryList;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import chihane.jdaddressselector.model.City;
import chihane.jdaddressselector.model.City_Table;
import chihane.jdaddressselector.model.County;
import chihane.jdaddressselector.model.County_Table;
import chihane.jdaddressselector.model.Province;
import chihane.jdaddressselector.model.Street;
import chihane.jdaddressselector.model.Street_Table;
import mlxy.utils.Lists;

public class AddressSelector implements AdapterView.OnItemClickListener {
    private static final int INDEX_TAB_PROVINCE = 0;
    private static final int INDEX_TAB_CITY = 1;
    private static final int INDEX_TAB_COUNTY = 2;
    private static final int INDEX_TAB_STREET = 3;

    private static final int INDEX_INVALID = -1;

    private final Context context;
    private final LayoutInflater inflater;
    private OnAddressSelectedListener listener;

    private View view;

    private LinearLayout layouttab;

    private TextView textViewProvince;
    private TextView textViewCity;
    private TextView textViewCounty;
    private TextView textViewStreet;

    private ListView listView;
    private ProvinceAdapter provinceAdapter;
    private CityAdapter cityAdapter;
    private CountyAdapter countyAdapter;
    private StreetAdapter streetAdapter;

    private FlowQueryList<Province> provinces;
    private FlowQueryList<City> cities;
    private FlowQueryList<County> counties;
    private FlowQueryList<Street> streets;

    private int provinceIndex = INDEX_INVALID;
    private int cityIndex = INDEX_INVALID;
    private int countyIndex = INDEX_INVALID;
    private int streetIndex = INDEX_INVALID;

    private int tabIndex = INDEX_TAB_PROVINCE;

    public AddressSelector(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);

        initViews();

        initAdapters();
        initProvince();
    }

    private void initAdapters() {
        provinceAdapter = new ProvinceAdapter();
        cityAdapter = new CityAdapter();
        countyAdapter = new CountyAdapter();
        streetAdapter = new StreetAdapter();
    }

    private void initProvince() {
        provinces = selectProvinces();
        provinceAdapter.notifyDataSetChanged();
        listView.setAdapter(provinceAdapter);

        updateTabsVisibility();
    }

    private void updateTabsVisibility() {
        textViewProvince.setVisibility(Lists.notEmpty(provinces) ? View.VISIBLE : View.GONE);
        textViewCity.setVisibility(Lists.notEmpty(cities) ? View.VISIBLE : View.GONE);
        textViewCounty.setVisibility(Lists.notEmpty(counties) ? View.VISIBLE : View.GONE);
        textViewStreet.setVisibility(Lists.notEmpty(streets) ? View.VISIBLE : View.GONE);

        textViewProvince.setEnabled(tabIndex != INDEX_TAB_PROVINCE);
        textViewCity.setEnabled(tabIndex != INDEX_TAB_CITY);
        textViewCounty.setEnabled(tabIndex != INDEX_TAB_COUNTY);
        textViewStreet.setEnabled(tabIndex != INDEX_TAB_STREET);
    }

    private void initViews() {
        view = inflater.inflate(R.layout.address_selector, null);
        this.listView = (ListView) view.findViewById(R.id.listView);
        this.layouttab = (LinearLayout) view.findViewById(R.id.layout_tab);

        this.textViewProvince = (TextView) view.findViewById(R.id.textViewProvince);
        this.textViewCity = (TextView) view.findViewById(R.id.textViewCity);
        this.textViewCounty = (TextView) view.findViewById(R.id.textViewCounty);
        this.textViewStreet = (TextView) view.findViewById(R.id.textViewStreet);

        this.textViewProvince.setOnClickListener(new OnProvinceTabClickListener());
        this.textViewCity.setOnClickListener(new OnCityTabClickListener());
        this.textViewCounty.setOnClickListener(new onCountyTabClickListener());
        this.textViewStreet.setOnClickListener(new OnStreetTabClickListener());

        this.listView.setOnItemClickListener(this);
    }

    class OnProvinceTabClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            tabIndex = INDEX_TAB_PROVINCE;
            listView.setAdapter(provinceAdapter);

            if (provinceIndex != INDEX_INVALID) {
                listView.setSelection(provinceIndex);
            }

            updateTabsVisibility();
        }
    }

    class OnCityTabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            tabIndex = INDEX_TAB_CITY;
            listView.setAdapter(cityAdapter);

            if (cityIndex != INDEX_INVALID) {
                listView.setSelection(cityIndex);
            }

            updateTabsVisibility();
        }
    }

    class onCountyTabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            tabIndex = INDEX_TAB_COUNTY;
            listView.setAdapter(countyAdapter);

            if (countyIndex != INDEX_INVALID) {
                listView.setSelection(countyIndex);
            }

            updateTabsVisibility();
        }
    }

    class OnStreetTabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            tabIndex = INDEX_TAB_STREET;
            listView.setAdapter(streetAdapter);

            if (streetIndex != INDEX_INVALID) {
                listView.setSelection(streetIndex);
            }

            updateTabsVisibility();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (tabIndex) {
            case INDEX_TAB_PROVINCE:
                Province province = provinceAdapter.getItem(position);

                // 更新当前级别及子级标签文本
                textViewProvince.setText(province.name);
                textViewCity.setText("请选择");
                textViewCounty.setText("请选择");
                textViewStreet.setText("请选择");

                // 更新子级数据
                cities = selectCitiesBy(province.id);
                counties = null;
                streets = null;
                cityAdapter.notifyDataSetChanged();
                countyAdapter.notifyDataSetChanged();
                streetAdapter.notifyDataSetChanged();

                // 更新选中数据
                this.provinceIndex = position;
                this.cityIndex = INDEX_INVALID;
                this.countyIndex = INDEX_INVALID;
                this.streetIndex = INDEX_INVALID;

                // 更新选中效果
                provinceAdapter.notifyDataSetChanged();

                if (Lists.notEmpty(cities)) {
                    // 以次级内容更新列表
                    listView.setAdapter(cityAdapter);
                    // 更新索引为次级
                    tabIndex = INDEX_TAB_CITY;
                } else {
                    // 次级无内容，回调
                    callbackInternal();
                }

                break;

            case INDEX_TAB_CITY:
                City city = cityAdapter.getItem(position);

                textViewCity.setText(city.name);
                textViewCounty.setText("请选择");
                textViewStreet.setText("请选择");

                counties = selectCountiesBy(city.id);
                streets = null;
                countyAdapter.notifyDataSetChanged();
                streetAdapter.notifyDataSetChanged();

                this.cityIndex = position;
                this.countyIndex = INDEX_INVALID;
                this.streetIndex = INDEX_INVALID;

                cityAdapter.notifyDataSetChanged();

                if (Lists.notEmpty(counties)) {
                    listView.setAdapter(countyAdapter);
                    tabIndex = INDEX_TAB_COUNTY;
                } else {
                    callbackInternal();
                }

                break;

            case INDEX_TAB_COUNTY:
                County county = countyAdapter.getItem(position);

                textViewCounty.setText(county.name);
                textViewStreet.setText("请选择");

                streets = selectStreetsBy(county.id);
                streetAdapter.notifyDataSetChanged();

                this.countyIndex = position;
                this.streetIndex = INDEX_INVALID;

                countyAdapter.notifyDataSetChanged();

                if (Lists.notEmpty(streets)) {
                    listView.setAdapter(streetAdapter);
                    tabIndex = INDEX_TAB_STREET;
                } else {
                    callbackInternal();
                }

                break;

            case INDEX_TAB_STREET:
                Street street = streetAdapter.getItem(position);

                textViewStreet.setText(street.name);

                streetAdapter.notifyDataSetChanged();

                this.streetIndex = position;

                callbackInternal();
                break;
        }

        updateTabsVisibility();
    }

    public View getView() {
        return view;
    }

    private void callbackInternal() {
        if (listener != null) {
            Province province = provinces == null || provinceIndex == INDEX_INVALID ? null : provinces.get(provinceIndex);
            City city = cities == null || cityIndex == INDEX_INVALID ? null : cities.get(cityIndex);
            County county = counties == null || countyIndex == INDEX_INVALID ? null : counties.get(countyIndex);
            Street street = streets == null || streetIndex == INDEX_INVALID ? null : streets.get(streetIndex);

            listener.onAddressSelected(
                    province,
                    city,
                    county,
                    street
            );
        }
    }

    private FlowQueryList<Province> selectProvinces() {
        return SQLite.select()
                .from(Province.class)
                .flowQueryList();
    }

    private FlowQueryList<City> selectCitiesBy(int provinceId) {
        return SQLite.select()
                .from(City.class)
                .where(City_Table.province_id.eq(provinceId))
                .flowQueryList();
    }

    private FlowQueryList<County> selectCountiesBy(int cityId) {
        return SQLite.select()
                .from(County.class)
                .where(County_Table.city_id.eq(cityId))
                .flowQueryList();
    }

    private FlowQueryList<Street> selectStreetsBy(int countyId) {
        return SQLite.select()
                .from(Street.class)
                .where(Street_Table.county_id.eq(countyId))
                .flowQueryList();
    }

    class ProvinceAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return provinces == null ? 0 : provinces.size();
        }

        @Override
        public Province getItem(int position) {
            return provinces.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_area, parent, false);

                holder = new Holder();
                holder.textView = (TextView) convertView.findViewById(R.id.textView);
                holder.imageViewCheckMark = (ImageView) convertView.findViewById(R.id.imageViewCheckMark);

                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

            Province item = getItem(position);
            holder.textView.setText(item.name);

            boolean checked = provinceIndex != INDEX_INVALID && provinces.get(provinceIndex).id == item.id;
            holder.textView.setEnabled(!checked);
            holder.imageViewCheckMark.setVisibility(checked ? View.VISIBLE : View.GONE);

            return convertView;
        }

        class Holder {
            TextView textView;
            ImageView imageViewCheckMark;
        }
    }

    class CityAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return cities == null ? 0 : cities.size();
        }

        @Override
        public City getItem(int position) {
            return cities.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_area, parent, false);

                holder = new Holder();
                holder.textView = (TextView) convertView.findViewById(R.id.textView);
                holder.imageViewCheckMark = (ImageView) convertView.findViewById(R.id.imageViewCheckMark);

                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

            City item = getItem(position);
            holder.textView.setText(item.name);

            boolean checked = cityIndex != INDEX_INVALID && cities.get(cityIndex).id == item.id;
            holder.textView.setEnabled(!checked);
            holder.imageViewCheckMark.setVisibility(checked ? View.VISIBLE : View.GONE);

            return convertView;
        }

        class Holder {
            TextView textView;
            ImageView imageViewCheckMark;
        }
    }

    class CountyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return counties == null ? 0 : counties.size();
        }

        @Override
        public County getItem(int position) {
            return counties.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_area, parent, false);

                holder = new Holder();
                holder.textView = (TextView) convertView.findViewById(R.id.textView);
                holder.imageViewCheckMark = (ImageView) convertView.findViewById(R.id.imageViewCheckMark);

                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

            County item = getItem(position);
            holder.textView.setText(item.name);

            boolean checked = countyIndex != INDEX_INVALID && counties.get(countyIndex).id == item.id;
            holder.textView.setEnabled(!checked);
            holder.imageViewCheckMark.setVisibility(checked ? View.VISIBLE : View.GONE);

            return convertView;
        }

        class Holder {
            TextView textView;
            ImageView imageViewCheckMark;
        }
    }

    class StreetAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return streets == null ? 0 : streets.size();
        }

        @Override
        public Street getItem(int position) {
            return streets.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_area, parent, false);

                holder = new Holder();
                holder.textView = (TextView) convertView.findViewById(R.id.textView);
                holder.imageViewCheckMark = (ImageView) convertView.findViewById(R.id.imageViewCheckMark);

                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

            Street item = getItem(position);
            holder.textView.setText(item.name);

            boolean checked = streetIndex != INDEX_INVALID && streets.get(streetIndex).id == item.id;
            holder.textView.setEnabled(!checked);
            holder.imageViewCheckMark.setVisibility(checked ? View.VISIBLE : View.GONE);

            return convertView;
        }

        class Holder {
            TextView textView;
            ImageView imageViewCheckMark;
        }
    }

    public interface OnAddressSelectedListener {
        void onAddressSelected(Province province, City city, County county, Street street);
    }

    public OnAddressSelectedListener getOnAddressSelectedListener() {
        return listener;
    }

    public void setOnAddressSelectedListener(OnAddressSelectedListener listener) {
        this.listener = listener;
    }
}