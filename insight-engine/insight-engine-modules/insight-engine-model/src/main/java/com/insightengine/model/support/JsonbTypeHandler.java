package com.insightengine.model.support;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * PostgreSQL {@code jsonb} 列的 String 读写处理器。
 *
 * <h3>为什么需要它</h3>
 * <p>MyBatis 默认用 {@code setString} 绑定参数，而 PostgreSQL 的 {@code jsonb} 列**不接受 varchar** 绑定
 * （会报 {@code column "rules" is of type jsonb but expression is of type character varying}）。
 * 常见解法有两种：① JDBC URL 加 {@code ?stringtype=unspecified}（全局放开类型推断，影响面大）；
 * ② 用 {@code PGobject}（要引入 driver 的编译期依赖）。本类走第三条路：
 * {@code setObject(i, value, Types.OTHER)} —— 让服务端按**目标列的真实类型**推断，效果等价且无需
 * 编译期依赖 driver（driver 在 pom 里是 runtime scope）。</p>
 *
 * <p>读取方向无需特殊处理：{@code jsonb → String} 直接 {@code getString} 即可。</p>
 */
@MappedTypes(String.class)
public class JsonbTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, parameter, Types.OTHER);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }
}
