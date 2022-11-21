<#setting locale="de_DE">
<#setting date_format="dd.MM.yyyy">
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Schichtplan Luftfracht Export</title>
    <style>
        @page {
            size: a4;
        }
        @media print {

        }

        html,
        body {
            margin: 0;
            padding: 0;
            height: 100%;
            font-size: 1.0em;
            background-color: #fff;
        }

        main {
            margin: 50px 100px 50px 100px;
        }

        header {
            margin: 50px 100px 0 100px;
        }

        section.legend {
            margin-bottom: 50px;
        }

        section.legend > h2 {
            font-size: small;
        }

        section.legend table {
            font-size: x-small;
        }

        section.legend table th {
            text-align: left;
            padding: 3px;
        }

        #shiftplan {
            width: 100%;
            border: 1px solid black;
            border-collapse: collapse;
            font-size: 1.0em;
        }

        #shiftplan > thead th {
            padding: 5px;
            border: 1px solid black;
        }

        #shiftplan > tbody tr:nth-child(odd) {
            background-color: #fff;
        }

        #shiftplan > tbody tr:nth-child(even) {
            background-color: #ddd;
        }

        #shiftplan > tbody th {
            padding: 5px;
            font-size: 0.9em;
            font-weight: normal;
            border: 1px solid black;
        }

        #shiftplan > tbody td {
            padding: 5px;
            font-size: 0.8em;
            border: 1px solid black;
        }
    </style>
</head>
<body>
<header>
    <h1>Schichtplan Luftfracht Export vom ${startDate.format("dd.MM.yyyy")} bis ${endDate.format("dd.MM.yyyy")}</h1>
</header>
<main>
    <section class="legend">
        <h2>Legende</h2>
        <table id="employee-details">
            <thead>
            <tr>
                <th>Mitarbeiter</th>
                <th>Homeoffice-Gruppe</th>
                <th>Homeoffice</th>
                <th>Spätdienst</th>
            </tr>
            </thead>
            <tbody>
            <#list employees as employee>
                <#assign isHomeOffice>
                    <#if employee.lateShiftOnly == false>
                        Ja
                    </#if>
                </#assign>
                <#assign isLateShift>
                    <#if employee.lateShiftOrder gte 0>
                        Ja
                    </#if>
                </#assign>
            <tr>
                <td style="color: ${employee.highlightColor};">${employee.name}</td>
                <td>${employee.employeeGroupName}</td>
                <td>${isHomeOffice!"Nein"}</td>
                <td>${isLateShift!"Nein"}</td>
            </tr>
            </#list>
            </tbody>
        </table>
    </section>
    <table id="shiftplan">
        <thead>
        <tr>
            <th>KW</th>
            <th>Von -> Bis</th>
            <th colspan="2">Montag</th>
            <th colspan="2">Dienstag</th>
            <th colspan="2">Mittwoch</th>
            <th colspan="2">Donnerstag</th>
            <th colspan="2">Freitag</th>
        </tr>
        </thead>
        <tbody>
        <tr>
            <th></th>
            <th></th>
            <th>Home Office</th>
            <th>Spätdienst</th>
            <th>Home Office</th>
            <th>Spätdienst</th>
            <th>Home Office</th>
            <th>Spätdienst</th>
            <th>Home Office</th>
            <th>Spätdienst</th>
            <th>Home Office</th>
            <th>Spätdienst</th>
        </tr>
        <#list calendar as calendarWeek>
            <tr>
                <td>${calendarWeek?index +1}</td>
                <td>${calendarWeek[0].format("dd.MM.")} - ${calendarWeek[6].format("dd.MM.")}</td>
            <#list calendarWeek as workday>
                <#if workday?index gt 4>
                    <#continue>
                </#if>
                <#if shiftPlan[workday]??>
                    <td>${shiftPlan[workday].homeOfficeGroup.groupName}</td>
                    <td style="color: ${shiftPlan[workday].lateShift.highlightColor}">${shiftPlan[workday].lateShift.name}</td>
                <#else>
                    <td colspan="2">Kein Arbeitstag</td>
                </#if>
            </#list>
            </tr>
        </#list>
        </tbody>
    </table>
</main>
</body>
</html>