import { Navigate, Route, Routes } from 'react-router-dom';
import LoginPage from '../pages/auth/LoginPage.jsx';
import RegisterPage from '../pages/auth/RegisterPage.jsx';
import HomePage from '../pages/home/HomePage.jsx';
import KnowledgePage from '../pages/discovery/KnowledgePage.jsx';
import FilterPage from '../pages/discovery/FilterPage.jsx';
import TalentPage from '../pages/talent/TalentPage.jsx';
import ApplyPage from '../pages/items/ApplyPage.jsx';
import MyItemsPage from '../pages/items/MyItemsPage.jsx';
import CreateMatterPage from '../pages/items/CreateMatterPage.jsx';
import MatterPage from '../pages/items/MatterPage.jsx';
import MessagesPage from '../pages/messages/MessagesPage.jsx';
import GroupChat from '../pages/messages/GroupChatPage.jsx';
import MyProfilePage from '../pages/profile/ProfilePage.jsx';
import SettingsPage from '../pages/profile/SettingsPage.jsx';
import WalletPage from '../pages/profile/WalletPage.jsx';
import CertificationPage from '../pages/certification/CertificationPage.jsx';
import CertificationApplyPage from '../pages/certification/CertificationApplyPage.jsx';
import CertificationUploadPage from '../pages/certification/CertificationUploadPage.jsx';
import NoticesPage from '../pages/notifications/NoticesPage.jsx';
import RatingPage from '../pages/support/RatingPage.jsx';
import FeedbackPage from '../pages/support/FeedbackPage.jsx';
import RulesPage from '../pages/rules/RulesPage.jsx';
import { ROUTES } from './routeConfig.js';

export default function AppRoutes(props) {
  const { go, notify } = props;
  const protect = (element) =>
    props.isAuthenticated ? element : <Navigate to={ROUTES.login} replace />;
  return (
    <Routes>
      <Route path={ROUTES.login} element={<LoginPage go={go} onLogin={props.login} />} />
      <Route path={ROUTES.register} element={<RegisterPage go={go} onRegister={props.login} />} />
      <Route
        path={ROUTES.home}
        element={protect(<HomePage go={go} setTalent={props.setTalent} />)}
      />
      <Route
        path={ROUTES.talent}
        element={protect(
          <TalentPage
            go={go}
            talent={props.talent}
            matter={props.matter}
            helpers={props.helpers}
            setHelpers={props.setHelpers}
          />,
        )}
      />
      <Route
        path={ROUTES.knowledge}
        element={
          <KnowledgePage
            go={go}
            category={props.category}
            setCategory={props.setCategory}
            problem={props.problem}
            setProblem={props.setProblem}
          />
        }
      />
      <Route
        path={ROUTES.filtered}
        element={
          <FilterPage
            go={go}
            problem={props.problem}
            setTalent={props.setTalent}
            title="按问题筛选"
            backScreen="knowledge"
          />
        }
      />
      <Route path={ROUTES.filter} element={<FilterPage go={go} setTalent={props.setTalent} />} />
      <Route
        path={ROUTES.apply}
        element={<ApplyPage go={go} selected={props.selected} setSelected={props.setSelected} />}
      />
      <Route
        path={ROUTES.requests}
        element={
          <MyItemsPage
            go={go}
            matter={props.matter}
            groups={props.groups}
            setSelectedGroup={props.setSelectedGroup}
          />
        }
      />
      <Route
        path={ROUTES.createMatter}
        element={
          <CreateMatterPage go={go} setMatter={props.setMatter} setHelpers={props.setHelpers} />
        }
      />
      <Route
        path={ROUTES.matter}
        element={
          <MatterPage
            go={go}
            matter={props.matter}
            helpers={props.helpers}
            setHelpers={props.setHelpers}
            notify={notify}
            hourlyFee={props.hourlyFee}
            balance={props.balance}
            setBalance={props.setBalance}
            setSelectedGroup={props.setSelectedGroup}
            setGroups={props.setGroups}
            setLedger={props.setLedger}
          />
        }
      />
      <Route path={ROUTES.rating} element={<RatingPage go={go} notify={notify} />} />
      <Route
        path={ROUTES.messages}
        element={protect(
          <MessagesPage
            go={go}
            groups={props.groups}
            setGroups={props.setGroups}
            setSelectedGroup={props.setSelectedGroup}
          />,
        )}
      />
      <Route
        path={ROUTES.chat}
        element={protect(<GroupChat go={go} notify={notify} group={props.selectedGroup} />)}
      />
      <Route
        path={ROUTES.profile}
        element={protect(
          <MyProfilePage go={go} hourlyFee={props.hourlyFee} logout={props.logout} />,
        )}
      />
      <Route
        path={ROUTES.settings}
        element={protect(
          <SettingsPage
            go={go}
            notify={notify}
            hourlyFee={props.hourlyFee}
            setHourlyFee={props.setHourlyFee}
            userSchedule={props.userSchedule}
            setUserSchedule={props.setUserSchedule}
          />,
        )}
      />
      <Route
        path={ROUTES.wallet}
        element={protect(
          <WalletPage
            go={go}
            notify={notify}
            balance={props.balance}
            setBalance={props.setBalance}
            ledger={props.ledger}
            setLedger={props.setLedger}
            withdrawals={props.withdrawals}
            setWithdrawals={props.setWithdrawals}
          />,
        )}
      />
      <Route path={ROUTES.rules} element={<RulesPage go={go} />} />
      <Route
        path={ROUTES.certs}
        element={<CertificationPage go={go} setCertType={props.setCertType} />}
      />
      <Route
        path={ROUTES.certApply}
        element={
          <CertificationApplyPage go={go} type={props.certType} setType={props.setCertType} />
        }
      />
      <Route
        path={ROUTES.certUpload}
        element={<CertificationUploadPage go={go} type={props.certType} notify={notify} />}
      />
      <Route path={ROUTES.feedback} element={<FeedbackPage go={go} notify={notify} />} />
      <Route path={ROUTES.notices} element={<NoticesPage go={go} />} />
      <Route path="*" element={<Navigate to={ROUTES.login} replace />} />
    </Routes>
  );
}
